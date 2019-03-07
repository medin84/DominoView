package kz.lof.taglib.api.view;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import javax.servlet.jsp.JspException;

import kz.lof.taglib.api.view.DominoViewTable.Pageable;
import lotus.domino.Base;
import lotus.domino.Document;
import lotus.domino.Item;
import lotus.domino.NotesException;
import lotus.domino.Session;
import lotus.domino.View;
import lotus.domino.ViewColumn;
import lotus.domino.ViewEntry;
import lotus.domino.ViewEntryCollection;
import lotus.domino.ViewNavigator;
import lotus.domino.taglib.ViewCollectionViewLoop;
import lotus.domino.taglib.ViewLoop;

public class DominoView {

	private static final String TAG = "DominoView";

	private static final String FONT_COLOR_WHITE = "#ffffff"; // 1
	private static final String FONT_COLOR_RED = "#ff0000"; // 2
	private static final String FONT_COLOR_GREEN = "#00ff00"; // 3
	private static final String FONT_COLOR_BLUE = "#0000ff"; // 4
	private static final String FONT_COLOR_MAGENTA = "#ff00ff"; // 5
	private static final String FONT_COLOR_YELLOW = "#ffff00"; // 6
	private static final String FONT_COLOR_CYAN = "#00ffff"; // 7
	private static final String FONT_COLOR_DARK_RED = "#800000"; // 8
	private static final String FONT_COLOR_DARK_GREEN = "#008000"; // 9
	private static final String FONT_COLOR_DARK_BLUE = "#000080"; // 10
	private static final String FONT_COLOR_DARK_MAGENTA = "#8b008b"; // 11
	private static final String FONT_COLOR_DARK_YELLOW = "#808000"; // 12
	private static final String FONT_COLOR_DARK_CYAN = "#008080"; // 13
	private static final String FONT_COLOR_GRAY = "#5f5f5f"; // 14
	private static final String FONT_COLOR_LIGHT_GRAY = "#808080"; // 15

	private static final char POS_SEPARATOR = '_';
	public static final String QUERY_PARAMETER_PAGE = "page";

	/**
	 * Создать постраничную навигацию в категории если (количество в категории -
	 * MIN_DIFF_PAGE) > отображать на странице по n.
	 */
	private static final int MIN_DIFF_PAGE = 5;

	private Session session;
	private View view;
	private ViewNavigator viewNav;
	private ViewLoop cachedViewLoop;
	private boolean useLoop;

	private DominoViewParam param;
	private DominoViewTable table;

	public DominoView(View view, DominoViewParam param) throws NotesException {
		this.param = param;
		this.view = view;
		session = view.getParent().getParent();
		param.setView(view);
	}

	public DominoViewParamDto getParam() {
		return new DominoViewParamDto(param);
	}

	public DominoViewTable getView() throws NotesException, JspException {

		table = new DominoViewTable();
		addViewColDesign(table);

		// check has visible document
		viewNav = view.createViewNav();
		ViewEntry viewEntry = viewNav.getFirstDocument();
		if (viewEntry == null) {
			return table;
		}

		if (view.isAutoUpdate()) {
			view.setAutoUpdate(false);
		}

		if (param.hasViewRefreshParam()) {
			view.refresh();
		}

		viewNav = createViewNav();

		if (useLoop) {
			getViewLoop().skipNItems(param.getStartIndex());
			viewEntry = getViewLoop().getNextViewEntry();
		} else {
			viewEntry = findEntry(viewNav, param.getStartIndex());
		}

		int topIndentLevel = 0;
		if (viewEntry != null) {
			topIndentLevel = viewEntry.getIndentLevel();
			if (!useLoop) {
				param.setTopStartPos(viewEntry.getPosition(POS_SEPARATOR));
			}
			int i = 0;

			while (param.isWholeView() || (i <= param.getItemsPerPage())) {
				if (!useLoop) {
					param.setTopEndPos(viewEntry.getPosition(POS_SEPARATOR));
				}

				getViewEntry(viewEntry);

				ViewEntry tempViewEntry = viewEntry;
				if (useLoop) {
					viewEntry = getViewLoop().getNextViewEntry();
				} else {
					viewEntry = viewNav.getNextSibling(viewEntry);
				}
				tempViewEntry.recycle();
				i++;

				if (viewEntry == null) {
					break;
				}
			}

			// Pageable
			if (viewEntry != null) {
				viewEntry = viewNav.getNextSibling(viewEntry);
				// check next entry for pagination
				if (viewEntry != null && viewEntry.getIndentLevel() <= topIndentLevel) {
					table.setPageable(new DominoViewTable.Pageable(QUERY_PARAMETER_PAGE, param.getPage(), true));
				}
			} else if (param.getPage() > 1) {
				table.setPageable(new DominoViewTable.Pageable(QUERY_PARAMETER_PAGE, param.getPage(), false));
			}
		}

		return table;
	}

	private void getViewEntry(ViewEntry viewEntry) throws NotesException {
		if (viewEntry == null) {
			return;
		}

		DominoViewTable.Row row = table.addRow();
		row.valid = viewEntry.isValid();

		if (row.valid) {
			row.indentLevel = viewEntry.getIndentLevel();
			row.pos = viewEntry.getPosition(POS_SEPARATOR);
			row.expanded = param.isExpandedPos(row.pos);

			Iterator<?> viewColValues = viewEntry.getColumnValues().iterator();

			if (viewEntry.isCategory()) {
				/*
				 * Category entry
				 */
				row.setType(DominoViewTable.RowType.CATEGORY);

				int colIndex = 1;
				int twistieCount = 0;
				while (viewColValues.hasNext()) {
					ViewColumn column = view.getColumn(colIndex);
					Object columnValue = viewColValues.next();

					if (!isHiddenColumn(column, colIndex)) {
						String val = getValue(viewEntry, column, null, columnValue, false, true);
						// check not categorized
						if (column.isShowTwistie() && column.isCategory()) {
							if (twistieCount == viewEntry.getIndentLevel() && val.isEmpty()) {
								val = param.getTextNotCategorized();
							}
							twistieCount++;
						}
						row.addCell(val);
					}
					colIndex++;
				}

			} else if (viewEntry.isDocument()) {
				/*
				 * Document entry
				 */
				Document doc = viewEntry.getDocument();
				boolean isResponse = doc.isResponse();

				if (isResponse) {
					row.setType(DominoViewTable.RowType.RESPONSE);
				} else {
					row.setType(DominoViewTable.RowType.DOCUMENT);
				}
				row.setUnid(doc.getUniversalID());
				row.setForm(doc.getItemValueString("Form"));

				// check child
				if (!useLoop) {
					ViewEntry _viewEntry = viewEntry;
					row.expandable = viewNav.gotoChild(viewEntry);
					viewEntry = _viewEntry;
				}

				// check formula
				if (param.isSelectable() && param.hasFormulaDisableSelection()) {
					if (((String) session.evaluate(param.getFormulaDisableSelection(), doc).get(0)).length() > 0) {
						row.disabled = true;
					}
				}

				int colIndex = 1;
				while (viewColValues.hasNext()) {
					ViewColumn column = view.getColumn(colIndex);
					Object columnValue = viewColValues.next();

					if (!isHiddenColumn(column, colIndex)) {
						String val = getValue(viewEntry, column, doc, columnValue, true, false);

						if (column.isHideDetail()) {
							row.addCell("");

						} else if (column.isCategory()) {
							row.addCell(param.getIndentValue());

						} else if (column.isResponse()) {
							if (isResponse) {
								row.addCell(val);
								break;
							} else {
								row.addCell(param.getIndentValue());
							}
						} else {
							row.addCell(val);
						}
					}
					colIndex++;
				}
				recycle(doc);

			} else if (viewEntry.isTotal()) {
				/*
				 * Total entry
				 */
				row.setType(DominoViewTable.RowType.TOTAL);

				int colIndex = 1;
				while (viewColValues.hasNext()) {
					ViewColumn column = view.getColumn(colIndex);
					Object columnValue = viewColValues.next();

					if (!isHiddenColumn(column, colIndex)) {
						row.addCell(getValue(viewEntry, column, null, columnValue, false, false));
					}
					colIndex++;
				}
			}
		}

		/*
		 * view entry nav
		 */
		if (!useLoop && row.expanded && viewNav.gotoChild(viewEntry)) {
			ViewEntry vce = viewNav.getCurrent();
			int siblingCount = 0;
			boolean isCategory = DominoViewTable.RowType.CATEGORY.name().equals(row.getType());
			DominoViewParam.EntryProps entryProps = null;

			if (isCategory) {
				// самая долгая операция является вычисление количества vce.getSiblingCount(),
				// поэтому в определенных условиях лучше кэшировать на странице
				entryProps = param.getEntryProps(vce.getPosition(POS_SEPARATOR));
				if (entryProps != null) {
					siblingCount = entryProps.getSiblingCount();
				}
			}

			if (siblingCount <= 0) {
				siblingCount = getSiblingCount(viewNav, vce, param.isShowSingleCategory());
			}

			if (siblingCount > 0) {
				try {
					/*
					 * Pagination in category
					 */
					int _rows = siblingCount;
					int subPage = 1;
					String subPos = null;

					if (param.isCategoryPageable() && isCategory && vce.isDocument()
							&& ((siblingCount - MIN_DIFF_PAGE) > param.getItemsPerPage())) {
						_rows = param.getItemsPerPage();
						int pageCount = calculatePageCount(siblingCount, _rows);

						if (pageCount > 1) {
							String vcePos = vce.getPosition(POS_SEPARATOR);
							String[] gotoPos = vcePos.split("" + POS_SEPARATOR);
							subPos = vcePos;
							subPage = param.getSubPage(vcePos);
							int _startEntry = Integer.valueOf(gotoPos[gotoPos.length - 1]);

							if (subPage > 1) {
								if (pageCount >= subPage) {
									_startEntry = _startEntry + ((subPage - 1) * _rows);
								} else {
									subPage = 1;
								}
							}
							//
							DominoViewTable.Row sr = table.addRow();
							sr.pageable = new Pageable(QUERY_PARAMETER_PAGE + vcePos, subPage, true);

							vce = findEntry(viewNav, _startEntry, vcePos, gotoPos);
						} else {
							_rows = siblingCount;
						}
					}
					// ------------------------

					String topPos = null;
					String botPos = null;
					if (subPos != null) {
						topPos = vce.getPosition(POS_SEPARATOR);
					}

					for (int i = 0; i < _rows; i++) {
						if (subPos != null) {
							botPos = vce.getPosition(POS_SEPARATOR);
						}

						getViewEntry(vce);

						ViewEntry tmpve = vce;
						vce = viewNav.getNextSibling(vce);
						recycle(tmpve);

						if (vce == null) {
							break;
						}
					}

					if (subPos != null) {
						if (!topPos.equals(botPos)) {
							/*
							 * subPagnCell.append("<input type='hidden' name='entryProps_" + subPos +
							 * "' value='" + siblingCount + "," + topPos + ":" + botPos + "," + subPage +
							 * "' />");
							 */
						}
					}
				} catch (Exception e) {
					// skip error
					e.printStackTrace();
				}
			} else {
				getViewEntry(vce);
			}

			recycle(vce);
		}
	}

	private ViewNavigator createViewNav() throws NotesException, JspException {

		if (isNotEmpty(param.getFullFtSearch())) {
			useLoop = true;
			table.getCols().forEach(col -> col.sort = null);
		}

		if (!useLoop && param.isLocalSession()) {
			viewNav.setMaxLevel(param.getViewNavMaxLevel());

			int colIndex = param.getSortColumnIndex();
			if (colIndex > -1 && (view.getColumnCount() >= colIndex)) {
				ViewColumn vc = view.getColumn(colIndex);
				if (vc != null && (vc.isResortAscending() || vc.isResortDescending())) {
					/*
					 * FTSearchSorted(query:string, maxdocs:int, column:int, ascending:boolean,
					 * exact:boolean, variants:boolean, fuzzy:boolean) : int
					 */
					boolean isAscending = DominoViewTable.Sort.ASC == param.getSortDirection();
					view.FTSearchSorted("*", 0, (colIndex - 1), isAscending, false, false, false);
					DominoViewTable.Col col = table.getCols().stream().filter(it -> it.index == colIndex).findFirst()
							.get();
					col.sorted = isAscending ? DominoViewTable.Sort.ASC : DominoViewTable.Sort.DESC;
				} else {
					param.setSortColumnIndex(-1);
				}
			}
		}

		if (param.isShowSingleCategory()) {
			return view.createViewNavFromCategory(param.getCategory());
		} else {
			if (useLoop) {
				param.setTopLevelEntryCount(getViewLoop().getTotalCount());
			}
			return view.createViewNav();
		}
	}

	private ViewEntry findEntry(ViewNavigator viewNav, int startEntry) throws NotesException {

		ViewEntry result = null;
		DominoViewParam.EntryProps props = param.getTopEntryProps();
		boolean fromBeginning = true;

		if (props != null && param.getPage() > 1) {
			fromBeginning = false;

			if (param.getPage() == props.getPrevViewedPage()) {
				// stay on page
				result = viewNav.getPos(props.getTopPos(), POS_SEPARATOR);

			} else if (param.getPage() > props.getPrevViewedPage()) {
				// Step ->
				int countStepForward = (param.getPage() - props.getPrevViewedPage() - 1) * param.getItemsPerPage();
				result = viewNav.getPos(props.getBotPos(), POS_SEPARATOR);
				for (int i = 0; i <= countStepForward; i++) {
					result = viewNav.getNextSibling(result);
				}

			} else if (param.getPage() < props.getPrevViewedPage()) {
				// Step <-
				int countStepBack = (props.getPrevViewedPage() - param.getPage()) * param.getItemsPerPage() - 1;
				int countStepFromBegin = startEntry;

				if (countStepBack > countStepFromBegin) {
					fromBeginning = true;
				} else {
					result = viewNav.getPos(props.getTopPos(), POS_SEPARATOR);
					for (int i = 0; i <= countStepBack; i++) {
						result = viewNav.getPrevSibling(result);
					}
				}
			}
		}

		if (fromBeginning) {
			result = viewNav.getFirst();
			for (int i = 0; i < startEntry; i++) {
				result = viewNav.getNextSibling(result);
			}
		}

		return result;
	}

	private ViewEntry findEntry(ViewNavigator viewNav, int startEntry, String pos, String[] gotoPos)
			throws NotesException {

		boolean fromBeginning = true;
		ViewEntry result = null;
		DominoViewParam.EntryProps entryProps = param.getEntryProps(pos);
		int sPage = param.getSubPage(pos);

		if (entryProps != null && sPage > 1) {
			fromBeginning = false;

			if (sPage == entryProps.getPrevViewedPage()) {
				// stay on page
				result = viewNav.getPos(entryProps.getTopPos(), POS_SEPARATOR);

			} else if (sPage > entryProps.getPrevViewedPage()) {
				// Step ->
				int countStepForward = (sPage - entryProps.getPrevViewedPage() - 1) * param.getItemsPerPage();
				result = viewNav.getPos(entryProps.getBotPos(), POS_SEPARATOR);
				for (int i = 0; i <= countStepForward; i++) {
					result = viewNav.getNextSibling(result);
				}

			} else if (sPage < entryProps.getPrevViewedPage()) {
				// Step <-
				int countStepBack = (entryProps.getPrevViewedPage() - sPage) * param.getItemsPerPage() - 1;
				int countStepFromBegin = startEntry;

				if (countStepBack > countStepFromBegin) {
					fromBeginning = true;
				} else {
					result = viewNav.getPos(entryProps.getTopPos(), POS_SEPARATOR);
					for (int i = 0; i <= countStepBack; i++) {
						result = viewNav.getPrevSibling(result);
					}
				}
			}
		}

		if (fromBeginning) {
			gotoPos[gotoPos.length - 1] = "" + startEntry;
			viewNav.gotoPos(join(Arrays.asList(gotoPos), "" + POS_SEPARATOR), POS_SEPARATOR);
			result = viewNav.getCurrent();
		}

		return result;
	}

	private ViewLoop getViewLoop() throws JspException {
		if (cachedViewLoop == null) {
			try {
				String i_key = null;
				String i_keySeparator = null;
				boolean i_keyExact = true;
				int i = (view.isHierarchical()) || (view.isCategorized()) || (view.isConflict()) ? 1 : 0;
				Object localObject1;
				boolean isSearch = param.getSearch() != null || isNotEmpty(param.getSearchQueryFilter());

				if ((i_key != null) || isSearch || (i == 0)) {
					localObject1 = null;
					Object localObject2 = null;
					Vector localVector = null;
					if (i_key != null) {
						if (i_keySeparator != null) {
							// localVector = parseMultiValueKey(this.i_key, this.i_keySeparator);
						} else {
							// i_keyType = this.i_thisTag.typeValue(this.i_keyTypeString,
							// "keytype");
							// localObject2 = getKeyValue(this.i_key);
						}
					}
					if ((localObject2 != null) || (localVector != null)) {
						if (localVector != null) {
							localObject1 = view.getAllEntriesByKey(localVector, i_keyExact);
						} else {
							localObject1 = view.getAllEntriesByKey(localObject2, i_keyExact);
						}
						if (isSearch) {
							if (param.getItemsPerPage() != -1) {
								((ViewEntryCollection) localObject1).FTSearch(param.getFullFtSearch(),
										param.getItemsPerPage());
							} else {
								((ViewEntryCollection) localObject1).FTSearch(param.getFullFtSearch());
							}
						}
					} else if (isSearch) {
						if (param.getItemsPerPage() != -1) {
							view.FTSearch(param.getFullFtSearch(), param.getItemsPerPage());
						} else {
							view.FTSearch(param.getFullFtSearch());
						}
						localObject1 = view.getAllEntries();
					} else {
						localObject1 = view.getAllEntries();
					}
					// cachedViewLoop = LoopFactory.createViewLoop((ViewEntryCollection)
					// localObject1, getItemsPerPage());
					cachedViewLoop = new ViewCollectionViewLoop((ViewEntryCollection) localObject1,
							param.getItemsPerPage());
				} else {
					int i_depth = -1;
					boolean i_toponly = true;

					if (param.getCategory() != null) {
						localObject1 = view.createViewNavFromCategory(param.getCategory());
						if (true) {
							((ViewNavigator) localObject1).setMaxLevel(0);
						} else if (i_depth != -1) {
							((ViewNavigator) localObject1).setMaxLevel(i_depth);
						}
					} else if (i_toponly) {
						localObject1 = view.createViewNavMaxLevel(0);
						// localObject1 = view.getAllEntries();
					} else if (i_depth != -1) {
						localObject1 = view.createViewNavMaxLevel(i_depth);
					} else {
						localObject1 = view.createViewNav();
					}
					cachedViewLoop = new ViewCollectionViewLoop((ViewEntryCollection) localObject1,
							param.getItemsPerPage());
					// cachedViewLoop = LoopFactory.createViewLoop((ViewNavigator) localObject1,
					// getRowCount());
				}
			} catch (NotesException e) {
				e.printStackTrace();
			}
		}
		return cachedViewLoop;
	}

	/**
	 * getValue
	 */
	private String getValue(ViewEntry ve, ViewColumn vc, Document dominoDoc, Object viewColumnValue, boolean isDocument,
			boolean isCategory) throws NotesException {
		String viewValueString;

		if (isDocument) {
			if (vc.isIcon()) {
				if (viewColumnValue instanceof Double) {
					int viewIntVal = ((Double) viewColumnValue).intValue();

					if (viewIntVal > 0) {
						if (viewIntVal >= 20 && viewIntVal <= 99) {
							viewValueString = "0" + viewIntVal;
						} else {
							viewValueString = String.valueOf(viewIntVal);
						}
					} else {
						viewValueString = "";
					}
				} else if ("0".equals((String) viewColumnValue)) {
					viewValueString = "";
				} else {
					if (viewColumnValue == null) {
						viewValueString = "";
					} else if (viewColumnValue instanceof Vector) {
						viewValueString = join((Vector) viewColumnValue, ",");
					} else {
						viewValueString = viewColumnValue.toString();
					}
				}

			} else if (vc.isField()) {
				if (dominoDoc.hasItem(vc.getFormula())) {
					Item itm = dominoDoc.getFirstItem(vc.getFormula());

					if (itm.getType() == 1024) {
						viewValueString = param.getDateFormat().format(itm.getDateTimeValue().toJavaDate());
					} else {
						viewValueString = itm.getText();
					}
				} else if (dominoDoc.hasItem(vc.getItemName())) {
					Item itm = dominoDoc.getFirstItem(vc.getItemName());

					if (itm.getType() == 1024) {
						viewValueString = param.getDateFormat().format(itm.getDateTimeValue().toJavaDate());
					} else {
						viewValueString = itm.getText();
					}
				} else if (viewColumnValue instanceof Vector) {
					viewValueString = join((Vector) viewColumnValue, ",");
				} else {
					viewValueString = viewColumnValue.toString();
				}

			} else if (vc.isFormula()) {
				String vcFormula = vc.getFormula();

				if (dominoDoc != null && vcFormula.startsWith("(") && vcFormula.endsWith(")")) {
					String itemName = vcFormula.substring(1, vcFormula.length() - 1);

					Item itm = dominoDoc.getFirstItem(itemName);
					if (itm != null) {
						if (itm.getType() == 1024) {
							viewValueString = param.getDateFormat().format(itm.getDateTimeValue().toJavaDate());
						} else {
							viewValueString = itm.getText();
						}
					} else {
						viewValueString = viewColumnValue.toString();
					}
				} else {
					if (viewColumnValue == null) {
						viewValueString = "";
					} else if (viewColumnValue instanceof Vector) {
						viewValueString = join((Vector) viewColumnValue, ",");
					} else {
						viewValueString = viewColumnValue.toString();
					}
				}
			} else {
				viewValueString = viewColumnValue.toString();
			}

		} else if (isCategory) {
			if (viewColumnValue == null && ve.isCategory()) {
				viewValueString = param.getTextIncorrectValue();

			} else if (viewColumnValue instanceof Double) {
				boolean formatPercent = false;
				boolean formatPunctuated = false;
				float f = Float.valueOf(viewColumnValue.toString());

				if (vc.getNumberAttrib() >= ViewColumn.ATTR_PERCENT) {
					formatPercent = true;
				}
				if (vc.getNumberAttrib() >= ViewColumn.ATTR_PUNCTUATED) {
					formatPunctuated = true;
				}

				if (formatPercent || formatPunctuated) {
					if (formatPunctuated) {
						// f = round(f, 2);
					}

					if (formatPercent) {
						if (f > 0) {
							f = round((f * 100), 2);
						}

						viewValueString = f + "%";
					} else {
						viewValueString = String.valueOf(f);
					}
				} else {
					viewValueString = String.valueOf((int) f);
				}
			} else {
				if (viewColumnValue == null) {
					viewValueString = "";
				} else {
					viewValueString = viewColumnValue.toString();
				}
			}
		} else if (ve.isTotal()) {
			if (viewColumnValue instanceof Double) {
				boolean formatPercent = false;
				boolean formatPunctuated = false;
				float f = Float.valueOf(viewColumnValue.toString());

				if (vc.getNumberAttrib() >= ViewColumn.ATTR_PERCENT) {
					formatPercent = true;
				}
				if (vc.getNumberAttrib() >= ViewColumn.ATTR_PUNCTUATED) {
					formatPunctuated = true;
				}

				if (formatPercent || formatPunctuated) {
					if (formatPunctuated) {
						// f = round(f, 2);
					}

					if (formatPercent) {
						if (f > 0) {
							f = round((f * 100), 2);
						}

						viewValueString = f + "%";
					} else {
						viewValueString = String.valueOf(f);
					}
				} else {
					viewValueString = String.valueOf((int) f);
				}
			} else {
				viewValueString = viewColumnValue.toString();
			}

		} else {
			if (viewColumnValue instanceof Double) {
				viewValueString = String.valueOf(((Double) viewColumnValue).intValue());
			} else {
				viewValueString = viewColumnValue.toString();
			}
		}

		return viewValueString;
	}

	private void addViewColDesign(DominoViewTable table) throws NotesException {

		Iterator<?> columns = view.getColumns().iterator();
		int columnIndex = 1;

		while (columns.hasNext()) {
			ViewColumn column = (ViewColumn) columns.next();
			if (!isHiddenColumn(column, columnIndex)) {
				DominoViewTable.Col col = table.addCol();

				col.isIcon = column.isIcon();
				col.isCategory = column.isCategory();
				col.isTwistie = column.isShowTwistie();
				col.isResponse = column.isResponse();
				col.isFontBold = column.isFontBold();
				col.isFontItalic = column.isFontItalic();
				col.isTotal = column.isHideDetail();
				col.index = columnIndex;
				col.title = column.getTitle();
				col.name = column.getItemName();
				col.alignment = column.getAlignment();
				col.color = getColorStyle(column.getFontColor());
				col.width = column.getWidth();
				col.resortToViewName = column.getResortToViewName();

				if (param.isLocalSession() && !useLoop) {
					if (column.isResortAscending() && column.isResortDescending()) {
						col.sort = DominoViewTable.Sort.BOTH;
					} else if (column.isResortAscending()) {
						col.sort = DominoViewTable.Sort.ASC;
					} else if (column.isResortDescending()) {
						col.sort = DominoViewTable.Sort.DESC;
					}
				}
			}
			columnIndex++;
		}
	}

	private int getSiblingCount(ViewNavigator nav, final ViewEntry entry, boolean showSingleCategory)
			throws NotesException {

		if (showSingleCategory) {
			return entry.getSiblingCount();
		} else {
			ViewEntry ve = entry;
			int count = 0;

			while (ve != null) {
				count++;
				ve = nav.getNextSibling(ve);
			}

			return count;
		}
	}

	private boolean isHiddenColumn(ViewColumn vc, int columnIndex) {
		try {
			if (vc.isResponse()) {
				return false;
			} else if (vc.isHidden()) {
				return true;
			} else if (param.getHiddenColumns().contains(columnIndex)) {
				return true;
			} else if (param.getHiddenColumns().contains(-1)) {
				return false;
			} else {
				return vc.isHideFormula();
			}
		} catch (Exception e) {
			// e.printStackTrace();
		}

		return false;
	}

	private static int calculatePageCount(int entryCount, int rows) {
		return (entryCount > rows) ? (int) Math.ceil((double) entryCount / rows) : 1;
	}

	private static float round(float number, int scale) {
		int pow = 10;
		for (int i = 1; i < scale; i++) {
			pow *= 10;
		}
		float tmp = number * pow;
		return (float) (int) ((tmp - (int) tmp) >= 0.5f ? tmp + 1 : tmp) / pow;
	}

	/**
	 * getColor
	 * 
	 * @param c int Domino color number
	 * @return String
	 */
	private static String getColor(int c) {
		switch (c) {
		case 1:
			return FONT_COLOR_WHITE;
		case 2:
			return FONT_COLOR_RED;
		case 3:
			return FONT_COLOR_GREEN;
		case 4:
			return FONT_COLOR_BLUE;
		case 5:
			return FONT_COLOR_MAGENTA;
		case 6:
			return FONT_COLOR_YELLOW;
		case 7:
			return FONT_COLOR_CYAN;
		case 8:
			return FONT_COLOR_DARK_RED;
		case 9:
			return FONT_COLOR_DARK_GREEN;
		case 10:
			return FONT_COLOR_DARK_BLUE;
		case 11:
			return FONT_COLOR_DARK_MAGENTA;
		case 12:
			return FONT_COLOR_DARK_YELLOW;
		case 13:
			return FONT_COLOR_DARK_CYAN;
		case 14:
			return FONT_COLOR_GRAY;
		case 15:
			return FONT_COLOR_LIGHT_GRAY;
		default:
			return "";
		}
	}

	private static String getColorStyle(int c) {
		return (c > 0 && c < 16) ? getColor(c) : "";
	}

	private static boolean isNotEmpty(String value) {
		return value != null && !value.isEmpty();
	}

	private static <T> String join(List<T> list, String sep) {
		if (list == null || list.size() == 0) {
			return "";
		}

		StringBuilder result = new StringBuilder(list.size() * 64);
		ListIterator<T> it = list.listIterator();

		while (it.hasNext()) {
			result.append(it.next());

			if (it.hasNext()) {
				result.append(sep);
			}
		}

		return result.toString();
	}

	private void recycle(Base obj) throws NotesException {
		if (obj != null) {
			obj.recycle();
		}
	}

	@Override
	public String toString() {
		return TAG;
	}
}
