package otgviewer.client.components;


import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.ProvidesResize;

/**
 * A screen manager provides screen management services.
 * @author johan
 *
 */
public interface ScreenManager extends ProvidesResize {

	/**
	 * Obtain the menu bar that menu entries can be installed on
	 * @return
	 */
	MenuBar getMenuBar();
	
	/**
	 * Indicate that the given screen is or is not configured.
	 * @param s
	 * @param configured
	 */
	void setConfigured(Screen s, boolean configured);
	
	/**
	 * Invalidate all screens' "configured" state and subsequently
	 * attempt their reconfiguration.
	 * Precondition: all screens must have been displayed at least once
	 * using Screen.show()
	 */
	void deconfigureAll(Screen from);
	
	/**
	 * Test whether the given screen is configured.
	 * @param key
	 * @return
	 */
	boolean isConfigured(String key);
	
	/**
	 * Try to proceed to a new screen, displaying it instead of the
	 * current one.
	 * @param to
	 */
	void attemptProceed(String to);
	
	/**
	 * A string uniquely identifying the user interface we wish to display.
	 * @return
	 */
	String getUIType();
	
}
