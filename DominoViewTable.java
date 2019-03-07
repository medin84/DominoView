package kz.lof.taglib.api.view;

import java.util.ArrayList;
import java.util.List;

public class DominoViewTable {

	public static enum RowType {
		CATEGORY, DOCUMENT, RESPONSE, TOTAL
	};

	public static enum Sort {
		ASC, DESC, BOTH
	};

	private List<Col> cols;
	private List<Row> rows;
	private int columnCount;
	private Pageable pageable;

	public DominoViewTable() {
		cols = new ArrayList<Col>();
		rows = new ArrayList<Row>();
	}

	public List<Col> getCols() {
		return cols;
	}

	public List<Row> getRows() {
		return rows;
	}

	public int getColumnCount() {
		return columnCount;
	}

	public Pageable getPageable() {
		return pageable;
	}

	public void setPageable(Pageable pageable) {
		this.pageable = pageable;
	}

	private void calcColumnCount() {
		int rowCount = rows.size();
		if (rowCount > 0) {
			int prevRowCellsSize = rows.get(rowCount - 1).getCells().size();
			if (columnCount < prevRowCellsSize) {
				columnCount = prevRowCellsSize;
			}
		}
	}

	public Col addCol() {
		Col col = new Col();
		cols.add(col);
		return col;
	}

	public Row addRow() {
		calcColumnCount();

		Row row = new Row();
		rows.add(row);
		return row;
	}

	public static class Col {

		public boolean isCategory;
		public boolean isResponse;
		public boolean isFontBold;
		public boolean isFontItalic;
		public boolean isTwistie;
		public boolean isIcon;
		public boolean isTotal;
		public int index;
		public Sort sort;
		public Sort sorted;
		public String title;
		public String name;
		public String resortToViewName;
		public String color;
		public int alignment;
		public int width;

		public Boolean isCategory() {
			return isCategory ? isCategory : null;
		}

		public Boolean isResponse() {
			return isResponse ? isResponse : null;
		}

		public Boolean isFontBold() {
			return isFontBold ? isFontBold : null;
		}

		public Boolean isFontItalic() {
			return isFontItalic ? isFontItalic : null;
		}

		public Boolean isTwistie() {
			return isTwistie ? isTwistie : null;
		}

		public Boolean isIcon() {
			return isIcon ? isIcon : null;
		}

		public Boolean isTotal() {
			return isTotal ? isTotal : null;
		}

		public int getIndex() {
			return index;
		}

		public String getSort() {
			return sort == null ? null : sort.name();
		}

		public String getSorted() {
			return sorted == null ? null : sorted.name();
		}

		public String getColor() {
			return color;
		}

		public String getTitle() {
			return title;
		}

		public String getName() {
			return name;
		}

		public String getResortToViewName() {
			return resortToViewName;
		}

		public int getAlignment() {
			return alignment;
		}

		public int getWidth() {
			return width;
		}

		public String getComputedWidth() {
			String result;

			if (isIcon) {
				result = "16px";
			} else if (isCategory) {
				result = "8px";
			} else if (isTotal) {
				result = "55px";
			} else {
				if (width < 10) {
					result = (width + 3) + "%";
				} else {
					if (width <= 15) {
						result = "15%";
					} else if (width > 15 && width <= 20) {
						result = "16%";
					} else if (width > 20 && width < 30) {
						result = "17%";
					} else if (width > 30 && width <= 40) {
						result = "18%";
					} else {
						result = "19%";
					}
				}
			}

			return result;
		}
	}

	public class Row {

		private RowType type;
		public boolean disabled;
		public boolean valid = true;
		public boolean expandable;
		public boolean expanded;
		public int indentLevel;

		public Pageable pageable;

		public String startPos;
		public String endPos;
		public String pos;

		private String unid;
		private String form;
		private List<String> cells;
		private List<Row> children;

		public Row() {
			cells = new ArrayList<String>();
		}

		public void addCell(String val) {
			cells.add(val);
		}

		public List<String> getCells() {
			if (type == RowType.CATEGORY || type == RowType.RESPONSE) {
				for (int i = cells.size() - 1; i > 0; i--) {
					if (!cells.get(i).isEmpty()) {
						return cells.subList(0, i + 1);
					}
				}
			}
			return cells;
		}

		public String getType() {
			return type.name();
		}

		public void setType(RowType type) {
			this.type = type;
		}

		public String getUnid() {
			return unid;
		}

		public void setUnid(String unid) {
			this.unid = unid;
		}

		public String getForm() {
			return form;
		}

		public void setForm(String form) {
			this.form = form;
		}

		public Boolean isValid() {
			return valid ? null : valid; // return only if false
		}

		public Boolean isDisabled() {
			return disabled ? disabled : null;
		}

		public String getStartPos() {
			return startPos;
		}

		public String getEndPos() {
			return endPos;
		}

		public String getPos() {
			return pos;
		}

		public Boolean isExpandable() {
			if (expandable || type == RowType.CATEGORY) {
				return true;
			}
			return null;
		}

		public Boolean isExpanded() {
			return expanded ? expanded : null;
		}

		public int getIndentLevel() {
			return indentLevel;
		}

		public List<Row> getChildren() {
			return children;
		}

		public Pageable getPageable() {
			return pageable;
		}
	}

	public static class Pageable {
		public String query;
		public boolean prev;
		public boolean next;
		public int page;

		public Pageable() {
		}

		public Pageable(int page, boolean next) {
			this.page = page;
			this.next = next;
		}

		public Pageable(String query, int page, boolean next) {
			this.query = query;
			this.page = page;
			this.next = next;
		}

		public String getQuery() {
			return query;
		}

		public boolean isPrev() {
			return page > 1;
		}

		public boolean isNext() {
			return next;
		}

		public int getPage() {
			return page;
		}
	}
}
