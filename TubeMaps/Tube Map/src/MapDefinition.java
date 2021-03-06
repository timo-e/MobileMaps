public class MapDefinition
{
	public MapDefinition()
	{
		description = "Central Tube Map";
		fileBase = "/central_tube_";
		xTiles = 12;
		yTiles = 10;
		tileWidth = 100;
		tileHeight = 100;
		totalWidth = 1200;
		totalHeight = 1000;
		xCentre = 650-125;
		yCentre = 530-125;
	}
	public int xTiles;
	public int yTiles;
	public int tileWidth;
	public int tileHeight;
	public int totalWidth;
	public int totalHeight;
	public int xCentre;
	public int yCentre;
	public String description;
	public String fileBase;
}