package  AntPheromones;


/**
* ObjectInGrid interface.
*
* objects must have:
* * set/get for a discrete x,y location
*/

public interface ObjectInGrid {

	public int getX();
	public void setX( int i );
	public int getY();
	public void setY( int i );
	
}
