package uiviewer.render;

public class SelectionState {
    private int selectedTileX = -1;
    private int selectedTileY = -1;
    private long selectedAgentId = -1;
    private int regionStartX = -1;
    private int regionStartY = -1;
    private int regionEndX = -1;
    private int regionEndY = -1;
    private boolean regionActive;

    public int getSelectedTileX() {
        return selectedTileX;
    }

    public int getSelectedTileY() {
        return selectedTileY;
    }

    public long getSelectedAgentId() {
        return selectedAgentId;
    }

    public boolean hasRegion() {
        return regionActive && regionStartX >= 0 && regionStartY >= 0 && regionEndX >= 0 && regionEndY >= 0;
    }

    public int getRegionStartX() {
        return regionStartX;
    }

    public int getRegionStartY() {
        return regionStartY;
    }

    public int getRegionEndX() {
        return regionEndX;
    }

    public int getRegionEndY() {
        return regionEndY;
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

    public void beginRegion(int startX, int startY) {
        this.regionStartX = startX;
        this.regionStartY = startY;
        this.regionEndX = startX;
        this.regionEndY = startY;
        this.regionActive = true;
    }

    public void updateRegionEnd(int endX, int endY) {
        if (!regionActive) {
            return;
        }
        this.regionEndX = endX;
        this.regionEndY = endY;
    }

    public void clearRegion() {
        this.regionStartX = -1;
        this.regionStartY = -1;
        this.regionEndX = -1;
        this.regionEndY = -1;
        this.regionActive = false;
    }
}
