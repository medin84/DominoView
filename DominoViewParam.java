package kz.lof.taglib.api.view;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import kz.lof.util.StringUtils;
import lotus.domino.NotesException;
import lotus.domino.View;

public class DominoViewParam {

	private HttpServletRequest request;

	private String id;
	private boolean isLocalSession;
	private int viewNavMaxLevel = 8;
	private String database;
	private String dbTitle;
	private String viewName;
	private String viewTitle;

	private int itemsPerPage = 20;
	private int page = 1;
	private Map<String, Integer> subPage = new HashMap<String, Integer>();
	private Map<String, EntryProps> entryProps;
	private int sortColumnIndex = -1;
	private DominoViewTable.Sort sortDirection;
	private Set<String> expandedEntries;
	private boolean hasViewRefreshParam;
	private int topLevelEntryCount = -1;

	private String topStartPos;
	private String topEndPos;

	private String category;
	private String search;
	private String searchQueryFilter = "";
	private Map<String, String> searchQueryFilterMap;

	private boolean isWholeView = false;
	private boolean expandAll;
	private boolean collapseAll;
	private boolean readOnly;
	private boolean isCategoryPageable = true;
	private boolean isSelectable = true;
	private boolean isMultiSelect = true;
	private String formulaDisableSelection = null;

	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
	private Set<Integer> hiddenColumns = new HashSet<>();
	private Map<Integer, String> widthColumns;

	private String indentValue = "#/";
	private String iconPath = "/SharedResources/xpage/img/icons/vwicn16/";
	private String iconPrfx = "vwicn";
	private String iconExt = ".png";

	private String textNoEntries = "No entries";
	private String textNotCategorized = "(Not Categorized)";
	private String textIncorrectValue = "(INCORRECT VALUE)";

	public DominoViewParam(HttpServletRequest request) throws NotesException {
		// checkIllegalQueryParameter(request);
		this.request = request;
	}

	public void setView(View view) throws NotesException {
		id = view.getUniversalID();
		database = view.getParent().getFilePath().replace("\\", "/");
		dbTitle = view.getParent().getTitle();
		viewName = view.getAliases().isEmpty() ? view.getName() : view.getAliases().firstElement().toString();
		viewTitle = view.getName();
	}

	public String getId() {
		return id;
	}

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public String getDbTitle() {
		return dbTitle;
	}

	public void setDbTitle(String dbTitle) {
		this.dbTitle = dbTitle;
	}

	public String getViewName() {
		return viewName;
	}

	public void setViewName(String viewName) {
		this.viewName = viewName;
	}

	public String getViewTitle() {
		return viewTitle;
	}

	public void setViewTitle(String viewTitle) {
		this.viewTitle = viewTitle;
	}

	public boolean isLocalSession() {
		return isLocalSession;
	}

	public void setIsLocalSession(boolean isLocalSession) {
		this.isLocalSession = isLocalSession;
	}

	public boolean isWholeView() {
		return isWholeView;
	}

	public SimpleDateFormat getDateFormat() {
		return dateFormat;
	}

	public void setDateFormat(SimpleDateFormat dateFormat) {
		this.dateFormat = dateFormat;
	}

	public boolean isSelectable() {
		return isSelectable;
	}

	public void setSelectable(boolean isSelectable) {
		this.isSelectable = isSelectable;
	}

	public boolean isMultiSelect() {
		return isMultiSelect;
	}

	public void setMultiSelect(boolean isMultiSelect) {
		this.isMultiSelect = isMultiSelect;
	}

	public DominoViewTable.Sort getSortDirection() {
		return sortDirection;
	}

	public void setSortDirection(DominoViewTable.Sort direction) {
		sortDirection = direction;
	}

	public int getSortColumnIndex() {
		return sortColumnIndex;
	}

	public void setSortColumnIndex(int index) {
		sortColumnIndex = index;
	}

	public boolean isShowSingleCategory() {
		return category != null && !category.isEmpty();
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getSearch() {
		return search;
	}

	public void setSearch(String search) {
		this.search = search;
	}

	public String getSearchQueryFilter() {
		return searchQueryFilter;
	}

	public void setSearchQueryFilter(String searchQueryFilter) {
		this.searchQueryFilter = searchQueryFilter;
	}

	public String getFullFtSearch() {
		String result;

		if (searchQueryFilter != null && !searchQueryFilter.isEmpty()) {
			result = getSearchQueryFilter() + (search != null ? " AND " + search + "*" : "");
		} else {
			result = (search != null ? search + "*" : "");
		}

		return result;
	}

	public int getItemsPerPage() {
		return itemsPerPage;
	}

	public void setItemsPerPage(int count) {
		itemsPerPage = count;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getSubPage(String pos) {
		if (!subPage.containsKey(pos)) {
			subPage.put(pos, getIntRequestParameter(DominoView.QUERY_PARAMETER_PAGE + pos, 1));
		}

		return subPage.get(pos);
	}

	public Set<String> getExpandedEntries() {
		return expandedEntries;
	}

	public void setExpandedEntries(Set<String> expandedEntries) {
		this.expandedEntries = expandedEntries;
	}

	public boolean hasViewRefreshParam() {
		return hasViewRefreshParam;
	}

	public void setHasViewRefreshParam(boolean hasViewRefreshParam) {
		this.hasViewRefreshParam = hasViewRefreshParam;
	}

	public int getTopLevelEntryCount() {
		return topLevelEntryCount;
	}

	public void setTopLevelEntryCount(int topLevelEntryCount) {
		this.topLevelEntryCount = topLevelEntryCount;
	}

	public int getViewNavMaxLevel() {
		return viewNavMaxLevel;
	}

	public int getStartIndex() {
		if (getPage() > 1) {
			return (getPage() - 1) * getItemsPerPage() + 1;
		}
		return 0;
	}

	public String getTopStartPos() {
		return topStartPos;
	}

	public void setTopStartPos(String pos) {
		topStartPos = pos;
	}

	public String getTopEndPos() {
		return topEndPos;
	}

	public void setTopEndPos(String pos) {
		topEndPos = pos;
	}

	public boolean isCategoryPageable() {
		return isCategoryPageable;
	}

	public void setCategoryPageable(boolean isCategoryPageable) {
		this.isCategoryPageable = isCategoryPageable;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public String getFormulaDisableSelection() {
		return formulaDisableSelection;
	}

	public void setFormulaDisableSelection(String formula) {
		this.formulaDisableSelection = formula;
	}

	public boolean hasFormulaDisableSelection() {
		return formulaDisableSelection != null && !formulaDisableSelection.isEmpty();
	}

	public Set<Integer> getHiddenColumns() {
		return hiddenColumns;
	}

	public void setHiddenColumns(Set<Integer> hiddenColumns) {
		this.hiddenColumns = hiddenColumns;
	}

	public Map<Integer, String> getWidthColumns() {
		return widthColumns;
	}

	public void setWidthColumns(Map<Integer, String> widthColumns) {
		this.widthColumns = widthColumns;
	}

	public boolean isExandAll() {
		return expandAll;
	}

	public void setExandAll(boolean expandAll) {
		this.expandAll = expandAll;
	}

	public boolean isCollapseAll() {
		return collapseAll;
	}

	public void setCollapseAll(boolean collapseAll) {
		this.collapseAll = collapseAll;
	}

	public String getIndentValue() {
		return indentValue;
	}

	public String getIconPath() {
		return iconPath;
	}

	public String getIconPrfx() {
		return iconPrfx;
	}

	public String getIconExt() {
		return iconExt;
	}

	public String getTextNoEntries() {
		return textNoEntries;
	}

	public void setTextNoEntries(String text) {
		textNoEntries = text;
	}

	public String getTextNotCategorized() {
		return textNotCategorized;
	}

	public String getTextIncorrectValue() {
		return textIncorrectValue;
	}

	public void setSearchQueryFilter(Map<String, String[]> filterRequest, String filterParamName) {
		searchQueryFilterMap = null;
		searchQueryFilter = "";

		if (filterRequest == null) {
			return;
		}

		List<String> filter = new ArrayList<String>();
		searchQueryFilterMap = new HashMap<String, String>();

		for (Entry<String, String[]> entry : filterRequest.entrySet()) {
			if (entry.getKey().startsWith(filterParamName + "[") && entry.getValue()[0].length() > 0) {
				String field = entry.getKey().substring(filterParamName.length());
				searchQueryFilterMap.put(field, entry.getValue()[0]);
				filter.add(field + "=\"" + entry.getValue()[0] + "\"");
			}
		}

		searchQueryFilter = StringUtils.join(filter, " AND ");
	}

	public boolean isExpandedPos(String pos) {
		if (isExandAll()) {
			return true;
		} else if (isCollapseAll()) {
			return false;
		} else {
			return expandedEntries.contains(getId() + pos);
		}
	}

	private int getIntRequestParameter(String paramName, int defaultValue) {
		String parValue = request.getParameter(paramName);
		if (parValue == null) {
			return defaultValue;
		}

		try {
			return Integer.valueOf(parValue);
		} catch (Exception e) {
			//
		}

		return defaultValue;
	}

	public EntryProps getTopEntryProps() {
		return getEntryProps("top");
	}

	public EntryProps getEntryProps(String pos) {
		if (entryProps == null) {
			entryProps = new HashMap<String, EntryProps>();
		}

		if (hasViewRefreshParam) {
			return null;
		}

		if (entryProps.containsKey(pos)) {
			return entryProps.get(pos);
		}

		String propsPar = request.getParameter("entryProps_" + pos);
		if (propsPar == null) {
			return null;
		}

		String[] propsArr = propsPar.split(",");
		int siblingCount = Integer.parseInt(propsArr[0]);
		String topPos = propsArr[1].split(":")[0];
		String botPos = propsArr[1].split(":")[1];
		int prevViewedPage = Integer.parseInt(propsArr[2]);

		EntryProps props = new EntryProps(siblingCount, topPos, botPos, prevViewedPage);
		entryProps.put(pos, props);

		return props;
	}

	public class EntryProps {

		private int siblingCount = -1;
		private String topPos;
		private String botPos;
		private int prevViewedPage;

		public EntryProps(int count, String topPos, String botPos, int page) {
			this.siblingCount = count > 1 ? count : -1;
			this.topPos = topPos;
			this.botPos = botPos;
			this.prevViewedPage = page;
		}

		public int getSiblingCount() {
			return siblingCount;
		}

		public String getTopPos() {
			return topPos;
		}

		public String getBotPos() {
			return botPos;
		}

		public int getPrevViewedPage() {
			return prevViewedPage;
		}

		@Override
		public String toString() {
			return siblingCount + "," + topPos + ":" + botPos + "," + prevViewedPage;
		}
	}
}
