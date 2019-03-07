package kz.lof.taglib.api.view;

public class DominoViewParamDto {

	private DominoViewParam param;

	public DominoViewParamDto(DominoViewParam param) {
		this.param = param;
	}

	public String getDatabase() {
		return param.getDatabase();
	}

	public String getDbTitle() {
		return param.getDbTitle();
	}

	public String getViewTitle() {
		return param.getViewTitle();
	}

	public String getViewName() {
		return param.getViewName();
	}

	public boolean isSelectable() {
		return param.isSelectable();
	}

	public boolean isMultiSelect() {
		return param.isMultiSelect();
	}

	public String getSearch() {
		return param.getSearch();
	}

	public String getSearchQueryFilter() {
		return param.getSearchQueryFilter();
	}

	public int getItemsPerPage() {
		return param.getItemsPerPage();
	}

	public int getPage() {
		return param.getPage();
	}

	public int getTopLevelEntryCount() {
		return param.getTopLevelEntryCount();
	}

	public String getTopStartPos() {
		return param.getTopStartPos();
	}

	public String getTopEndPos() {
		return param.getTopEndPos();
	}

	public boolean isReadOnly() {
		return param.isReadOnly();
	}

	public String getIndentValue() {
		return param.getIndentValue();
	}

	public String getIconPath() {
		return param.getIconPath();
	}

	public String getIconPrfx() {
		return param.getIconPrfx();
	}

	public String getIconExt() {
		return param.getIconExt();
	}

	public String getTextNoEntries() {
		return param.getTextNoEntries();
	}

	public String getTextNotCategorized() {
		return param.getTextNotCategorized();
	}

	public String getTextIncorrectValue() {
		return param.getTextIncorrectValue();
	}

	public DominoViewParam.EntryProps getTopEntryProps() {
		return param.getEntryProps("top");
	}
}
