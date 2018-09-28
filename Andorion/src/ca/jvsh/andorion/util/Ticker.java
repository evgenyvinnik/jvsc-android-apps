package ca.jvsh.andorion.util;

/**
 * Base interface for the ticker we use to control the animation.
 */
public interface Ticker
{
	// Stop this thread.  There will be no new calls to tick() after this.
	public void kill();

	// Stop this thread and wait for it to die.  When we return, it is
	// guaranteed that tick() will never be called again.
	// 
	// Caution: if this is called from within tick(), deadlock is
	// guaranteed.
	public void killAndWait();

	// Run method for this thread -- simply call tick() a lot until
	// enable is false.
	public void run();

	// Determine whether this ticker is still going.
	public boolean isAlive();
	
	//set how often to call tick function
	public void setAnimationDelay(long animationDelay);

}