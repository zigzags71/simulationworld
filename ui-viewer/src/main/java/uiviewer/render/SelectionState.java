package uiviewer.render;

public class SelectionState {
    private int selectedTileX = -1;
    private int selectedTileY = -1;
    private long selectedAgentId = -1;

    public int getSelectedTileX() {
        return selectedTileX;
    }

    public int getSelectedTileY() {
        return selectedTileY;
    }

    public long getSelectedAgentId() {
        return selectedAgentId;
    }

    public void setSelectedTile(int x, int y) {
        this.selectedTileX = x;
        this.selectedTileY = y;
    }

    public void clearTile() {
        this.selectedTileX = -1;
        this.selectedTileY = -1;
    }

    public void setSelectedAgentId(long selectedAgentId) {
        this.selectedAgentId = selectedAgentId;
    }

    public void clearAgent() {
        this.selectedAgentId = -1;
    }
}
