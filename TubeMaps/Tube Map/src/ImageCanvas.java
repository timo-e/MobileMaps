import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.Sprite;
import java.util.*;
import java.lang.Math;

// This class displays a selected image centered in the screen
class ImageCanvas extends Canvas implements ImageCache.ImageLoadedNotification
{
	// The map definition.
	private MapDefinition def;
	// The image cache.
	private ImageCache cache;
	
	// The position of the screen centre.
	private int xPos;
	private int yPos;

	// Screen size.
	private int width;
	private int height;

	// The timer to update the scrolling.
	private Timer timer = new Timer();
	
	// The timer task. Also records where we are in the animation etc.
    private Scroller scroller = new Scroller();
	
	// How we animate the movement.
	private int[] animationCurve = { 40, 80, 100, 120, 100, 80, 40 };
	private int animationCurveSum = 40 + 80 + 100 + 120 + 100 + 80 + 40;
	
    class Scroller extends TimerTask
	{
		private Vector xMoves = new Vector();
		private Vector yMoves = new Vector();
		
		// Return true if we are still scrolling.
		public boolean isScrolling()
		{
			return !xMoves.isEmpty() || !yMoves.isEmpty();
		}
		
		public void addXMovement(int[] x)
		{
			if (xMoves.size() < x.length)
				xMoves.setSize(x.length);
			for (int i = 0; i < x.length; ++i)
			{
				if (xMoves.elementAt(i) == null)
					xMoves.setElementAt(new Integer(x[i]), i);
				else
					xMoves.setElementAt(new Integer(((Integer)xMoves.elementAt(i)).intValue() + x[i]), i);
			}
		}
		public void addYMovement(int[] y)
		{
			if (yMoves.size() < y.length)
				yMoves.setSize(y.length);
			for (int i = 0; i < y.length; ++i)
			{
				if (yMoves.elementAt(i) == null)
					yMoves.setElementAt(new Integer(y[i]), i);
				else
					yMoves.setElementAt(new Integer(((Integer)yMoves.elementAt(i)).intValue() + y[i]), i);
			}
		}
		
		public void run()
		{
			if (!xMoves.isEmpty())
			{
				setXPos(getXPos() + ((Integer)xMoves.elementAt(0)).intValue());
				xMoves.removeElementAt(0);
			}
			
			if (!yMoves.isEmpty())
			{
				setYPos(getYPos() + ((Integer)yMoves.elementAt(0)).intValue());
				yMoves.removeElementAt(0);
			}
			
			if (!isScrolling())
				cancel();
			
			repaint();
        }
	}
	
	ImageCanvas()
	{
		xPos = 0;
		yPos = 0;

		width = getWidth();
		height = getHeight();
	}
	
	public void displayMap(MapDefinition def, int xPos, int yPos)
	{		
		this.def = def;
		
		if (def == null)
			return;
		
		this.xPos = xPos;
		this.yPos = yPos;
		
		cache = new ImageCache(def.xTiles * def.yTiles, def.fileBase, 0);
		cache.setNotify(this);
		
		// Preload the initial images.
		
		int l = xPos - width/2;
		int r = xPos + width/2;
		int t = yPos - height/2;
		int b = yPos + height/2;
				
		int xStart = l / def.tileWidth;
		int yStart = t / def.tileHeight;
		
		int xStop = r / def.tileWidth;
		int yStop = b / def.tileHeight;
		
		for (int x = 0; x < def.xTiles; ++x)
			for (int y = 0; y < def.yTiles; ++y)
				if (x >= xStart && x <= xStop && y >= yStart && y <= yStop)
					cache.preloadImage(toIndex(x, y));
		
		repaint();
	}
	
	protected void paint(Graphics g)
	{
		int margin = 10; // Reduce flicker slightly.

		int l = xPos - width/2 - margin;
		int r = xPos + width/2 + margin;
		int t = yPos - height/2 - margin;
		int b = yPos + height/2 + margin;
				
		g.setColor(0xFFFFFF);
		g.fillRect(0, 0, width, height);
		
		if (def == null)
			return;
		
		// Work out which tiles we need and load them (and release the others).
		int xStart = l / def.tileWidth;
		int yStart = t / def.tileHeight;
		
		int xStop = r / def.tileWidth;
		int yStop = b / def.tileHeight;
		
		for (int x = 0; x < def.xTiles; ++x)
		{
			for (int y = 0; y < def.yTiles; ++y)
			{
				if (x >= xStart && x <= xStop && y >= yStart && y <= yStop)
				{
					cache.setDesired(toIndex(x, y), true);
					
					Image tile = cache.getImage(toIndex(x, y));
					
					if (tile != null)
						g.drawRegion(tile, 0, 0, tile.getWidth(), tile.getHeight(), Sprite.TRANS_NONE,
								 	x * def.tileWidth - xPos + width/2, y * def.tileHeight - yPos + height/2, 0);
				}
				else
				{
					cache.setDesired(toIndex(x, y), false);
				}
			}
		}
	}
	
	public void imageLoaded(int imNum)
	{
		if (!scroller.isScrolling())
			repaint(); // TODO: Just repaint that image area.
	}
	
	private void addAnim(int dx, int dy)
	{
		boolean doSchedule = false;
		if (dx != 0 || dy != 0 && !scroller.isScrolling())
		{
			// We have to make a whole new object. So much for java not sucking.
			scroller = new Scroller();
			doSchedule = true;
		}
		
		if (dx != 0)
		{
			int[] addX = new int[animationCurve.length];
			for (int i = 0; i < animationCurve.length; ++i)
			{
				addX[i] = dx * animationCurve[i] / animationCurveSum;
			}
			scroller.addXMovement(addX);
		}
		
		if (dy != 0)
		{
			int[] addY = new int[animationCurve.length];
			for (int i = 0; i < animationCurve.length; ++i)
			{
				addY[i] = dy * animationCurve[i] / animationCurveSum;
			}
			scroller.addYMovement(addY);
		}
		
		if (doSchedule)
			timer.schedule(scroller, 0, 1000/30);
	}

	protected void keyPressed(int keyCode)
	{
		if (def == null)
			return;

		int amount = Math.min(getWidth(), getHeight()) / 2;

		switch (getGameAction(keyCode))
		{
			case RIGHT:
				addAnim(amount, 0);
				break;
			case LEFT:
				addAnim(-amount, 0);
				break;
			case DOWN:
				addAnim(0, amount);
				break;
			case UP:
				addAnim(0, -amount);
				break;
			default:
				switch (keyCode)
				{
					case KEY_NUM1:
						addAnim(-amount, -amount);
						break;
					case KEY_NUM2:
						addAnim(0, -amount);
						break;
					case KEY_NUM3:
						addAnim(amount, -amount);
						break;
					case KEY_NUM4:
						addAnim(-amount, 0);
						break;
					case KEY_NUM5:
						int xPosNew = def.xCentre;
						int yPosNew = def.yCentre;
						addAnim(xPosNew - xPos, yPosNew - yPos); // Not quite right!
						break;
					case KEY_NUM6:
						addAnim(amount, 0);
						break;
					case KEY_NUM7:
						addAnim(-amount, amount);
						break;
					case KEY_NUM8:
						addAnim(0, amount);
						break;
					case KEY_NUM9:
						addAnim(amount, amount);
						break;
				}
				break;
		}
	}
	
	protected void keyRepeated(int keyCode)
	{
		keyPressed(keyCode);
	}
	
	private int toIndex(int x, int y)
	{
		return x + y * def.xTiles;
	}	

	public void setXPos(int x)
	{
		xPos = x;
		if (xPos < width/2)
			xPos = width/2;
		if (xPos > def.totalWidth - width/2)
			xPos = def.totalWidth - width/2;
	}
	
	public void setYPos(int y)
	{
		yPos = y;
		if (yPos < height/2)
			yPos = height/2;
		if (yPos > def.totalHeight - height/2)
			yPos = def.totalHeight - height/2;
	}
	
	public int getXPos()
	{
		return xPos;
	}
	
	public int getYPos()
	{
		return yPos;
	}
}

