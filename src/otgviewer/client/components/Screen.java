package otgviewer.client.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import otgviewer.client.Resources;
import otgviewer.client.SampleDetailScreen;
import otgviewer.client.Utils;
import otgviewer.shared.Barcode;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style.Float;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;


/**
 * Part of a sequence of screens in a workflow.
 * Each screen knows what its parent is, and renders a sequence of links to all the parents.
 * @author johan
 *
 */
public class Screen extends DataListenerWidget implements RequiresResize, ProvidesResize {
	protected static Resources resources = GWT.create(Resources.class);
	
	protected DockLayoutPanel rootPanel;
	private String key; //An identifier string

	private FlowPanel statusPanel;		
	protected boolean visible = false;
	private Label viewLabel = new Label();
	private boolean showDataFilter = false, showGroups = false;
	private MenuBar menuBar;	
	protected boolean configured = false;
	private List<MenuItem> menuItems = new ArrayList<MenuItem>();
	private Widget bottom;
	private HorizontalPanel spOuter, guideBar;
	private List<Widget> toolbars = new ArrayList<Widget>();
	private List<Widget> leftbars = new ArrayList<Widget>();	
	private boolean showGuide;
	
	protected ScreenManager manager;
	
	protected TextResource helpHTML;
	protected ImageResource helpImage;
	
	public abstract static class QueuedAction implements Runnable {
		String name;
		public QueuedAction(String name) {
			this.name = name;
		}
		
		public int hashCode() {
			return name.hashCode();
		}
		
		public boolean equals(Object other) {
			if (other instanceof QueuedAction) {
				return name.equals(((QueuedAction) other).name);
			}
			return false;
		}
		
		abstract public void run();
	}
	
	private Set<QueuedAction> actionQueue = new HashSet<QueuedAction>(); 
	
	
	public Screen(String title, String key,  
			boolean showDataFilter, boolean showGroups, 
			ScreenManager man,
			TextResource helpHTML, ImageResource helpImage) {
		this.showDataFilter = showDataFilter;
		this.showGroups = showGroups;
		this.helpHTML = helpHTML;
		this.helpImage = helpImage;		
		rootPanel = new DockLayoutPanel(Unit.PX);
		
		initWidget(rootPanel);
		menuBar = man.getMenuBar();
		manager = man;				
		viewLabel.setWordWrap(false);
		viewLabel.getElement().getStyle().setMargin(2, Unit.PX);
		this.key = key;
		
		setTitle(title);		
	}
	
	public Screen(String title, String key,  
			boolean showDataFilter, boolean showGroups, ScreenManager man) {
		this(title, key, showDataFilter, showGroups, man, null, null);
	}
	
	public ScreenManager manager() {
		return this.manager;
	}
	
	/**
	 * Is this screen ready for use?
	 * @return
	 */
	public boolean enabled() {
		return true;
	}
	
	/**
	 * Has the user finished configuring this screen?
	 * @return
	 */
	public boolean configured() {
		return configured;
	}
	
	/**
	 * For subclass implementations to indicate that they have been configured
	 */
	public void setConfigured(boolean cfg) {
		configured = cfg;
		manager.setConfigured(this, configured);
	}
	
	/**
	 * Subclass implementations should use this method to check
	 * whether sufficient state to be "configured" has been loaded.
	 * If it has, they should call setConfigured().
	 */
	public void tryConfigure() {
		setConfigured(true);
	}
		
	protected void configuredProceed(String key) {
		setConfigured(true);
		manager.attemptProceed(key);		
	}
	
	public void initGUI() {
		statusPanel = new FlowPanel(); 
		statusPanel.setStyleName("statusPanel");		
		floatLeft(statusPanel);

		spOuter = Utils.mkWidePanel();		
		spOuter.setHeight("30px");
		spOuter.add(statusPanel);		
		spOuter.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
		statusPanel.setStyleName("statusPanel");
		spOuter.setStyleName("statusPanel");	

		guideBar = Utils.mkWidePanel();
		guideBar.setHeight("30px");
		guideBar.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
		guideBar.setStyleName("guideBar");
		guideBar.add(mkGuideTools()); 
		
		addToolbars(); //must be called before rootPanel.add()		
		bottom = bottomContent();
		if (bottom != null) {
			HorizontalPanel hp = Utils.mkWidePanel();			
			hp.add(bottom);
			hp.setHeight("40px");
			rootPanel.addSouth(hp, 40);
		}
		rootPanel.add(content());
	}
	
	private Widget mkGuideTools() {		
		Label l = new Label(getGuideText());
		floatLeft(l);
		HorizontalPanel hp = Utils.mkWidePanel();
		hp.add(l);
		
		HorizontalPanel hpi = new HorizontalPanel();
		
		PushButton i;
		if (helpAvailable()) {
			i = new PushButton(new Image(resources.help()));
			i.setStyleName("slightlySpaced");
			i.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					showHelp();
				}
			});
			hpi.add(i);
		}
		
		i = new PushButton(new Image(resources.close()));
		i.setStyleName("slightlySpaced");
		i.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				hideToolbar(guideBar);
				showGuide = false;
				storeState();
			}			
		});		
		hpi.add(i);		
		
		floatRight(hpi);
		hp.add(hpi);
		
		return hp;
	}
	
	public void showGuide() {
		showToolbar(guideBar);
		showGuide = true;
		storeState();
	}
	
	protected void addToolbars() {
		addToolbar(guideBar, 40);
		if (!showGuide) {
			guideBar.setVisible(false);
		} 
		addToolbar(spOuter, 40);
	}
	
	/**
	 * This method will be called each time the screen is displayed anew.
	 * If overriding, make sure to call the superclass method.
	 */
	public void show() {
		rootPanel.forceLayout();
		visible = true;		
		for (MenuItem mi: menuItems) {
			mi.setVisible(true);
		}
		loadState();
		if (showGuide) {
			showToolbar(guideBar);
		} else {
			hideToolbar(guideBar);
		}
		updateStatusPanel(); //needs access to the groups from loadState
		runActions();
		deferredResize();
	}
	
	@Override
	public void loadState(Storage s) {
		super.loadState(s);
		String v = s.getItem("OTG.showGuide");
		if (v == null || v.equals("yes")) {
			showGuide = true;
		} else {
			showGuide = false;
		}
	}
	
	@Override
	public void storeState(Storage s) {
		super.storeState(s);
		if (showGuide) {
			s.setItem("OTG.showGuide", "yes");
		} else {
			s.setItem("OTG.showGuide", "no");
		}
	}
	
	private void runActions() {
		for (QueuedAction qa: actionQueue) {
			qa.run();
		}
		actionQueue.clear();
	}
	
	public void enqueue(QueuedAction qa) {
		actionQueue.remove(qa); //remove it if it's already there (so we can update it)
		actionQueue.add(qa);
	}

	private void floatRight(Widget w) {
		w.getElement().getStyle().setFloat(Float.RIGHT);
	}
	private void floatLeft(Widget w) {
		w.getElement().getStyle().setFloat(Float.LEFT);
	}
	private void floatLeft(FlowPanel fp, Widget w) {
		floatLeft(w);
		fp.add(w);
	}
	
	protected void updateStatusPanel() {
//		statusPanel.setWidth(Window.getClientHeight() + "px");
		statusPanel.clear();
		statusPanel.add(viewLabel);
		floatLeft(viewLabel);
//		viewLabel.getElement().getStyle().setFloat(Float.LEFT);
		if (showGroups) {
			Collections.sort(chosenColumns);
			
			for (Group g: chosenColumns) {				
				FlowPanel fp = new FlowPanel(); 
				fp.setStyleName("statusBorder");
				String tip = g.getCDTs(-1, ", ");
				Label l = Utils.mkEmphLabel(g.getName() + ":");
				l.setWordWrap(false);
				l.getElement().getStyle().setMargin(2, Unit.PX);
				l.setStyleName(g.getStyleName());
				floatLeft(fp, l);
				l.setTitle(tip);
				l = new Label(g.getCDTs(2, ", "));
				l.getElement().getStyle().setMargin(2, Unit.PX);
				l.setStyleName(g.getStyleName());
				floatLeft(fp, l);
				l.setTitle(tip);
				l.setWordWrap(false);
				floatLeft(statusPanel, fp);				
			}
		}		
	}
	
	public void resizeInterface() {		
		for (Widget w: toolbars) {						
			rootPanel.setWidgetSize(w, w.getOffsetHeight());			
		}
		for (Widget w: leftbars) {
			rootPanel.setWidgetSize(w, w.getOffsetWidth());
		}
		rootPanel.forceLayout();
//		rootPanel.setWidgetSize(spOuter, statusPanel.getOffsetHeight() + 10);
	}
	
	protected void addToolbar(Widget toolbar, int size) {
		toolbars.add(toolbar);
		rootPanel.addNorth(toolbar, size);		
	}
	
	public void showToolbar(Widget toolbar) {
		showToolbar(toolbar, toolbar.getOffsetHeight());
	}
	
	public void showToolbar(Widget toolbar, int size) {
		toolbar.setVisible(true);
		rootPanel.setWidgetSize(toolbar, size);		
		deferredResize();
	}
	
	public void hideToolbar(Widget toolbar) {
		toolbar.setVisible(false);
		deferredResize();
	}
	
	protected void addLeftbar(Widget leftbar, int size) {
//		leftbars.add(leftbar);
		rootPanel.addWest(leftbar, size);
	}
	
	//Sometimes we need to do a deferred resize, because the layout engine has not finished yet
	//at the time when we request the resize operation.
	public void deferredResize() {
		Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand () {
			public void execute() {
				resizeInterface();						
			}
		});
	}
	
	/**
	 * This method will be called each time the screen is hidden.
	 * If overriding, make sure to call the superclass method.
	 */
	public void hide() {		
		visible = false;
		for (MenuItem mi: menuItems) {
			mi.setVisible(false);
		}
	}
	
	public void addMenu(MenuItem m) {
		menuBar.addItem(m);
		m.setVisible(false);
		menuItems.add(m);
	}

	
	/**
	 * Override this method to define the main content of the screen.
	 * Stored state may not have been loaded when this method is invoked.
	 * @return
	 */
	public Widget content() {
		return new SimplePanel();
	}
	
	public Widget bottomContent() {
		return null;
	}
	
	public String key() {
		return key;
	}
	
	@Override
	public void dataFilterChanged(DataFilter filter) {
		super.dataFilterChanged(filter);
		if (showDataFilter) {
			viewLabel.setText(filter.toString());
		}
	}
	
	public boolean helpAvailable() {
		return helpHTML != null;
	}
	
	public void showHelp() {
		Utils.showHelp(getHelpHTML(), getHelpImage());		
	}
	
	protected TextResource getHelpHTML() {
		if (helpHTML == null) {
			return resources.defaultHelpHTML();
		} else {
			return helpHTML;
		}
	}
	
	protected ImageResource getHelpImage() {
		return helpImage;	
	}
	
	/**
	 * The text that is displayed to first-time users on each screen to assist them.
	 * @return
	 */
	protected String getGuideText() {
		return "Use Instructions on the Help menu to get more information.";
	}

	@Override
	public void onResize() {
		final int c = rootPanel.getWidgetCount();
		for (int i = 0; i < c; ++i) {
			Widget w = rootPanel.getWidget(i);
			if (w instanceof RequiresResize) {
				((RequiresResize) w).onResize();
			}
		}		
	}
	
	//TODO: best location for this?
	public void displaySampleDetail(Barcode b) {
		Storage s = tryGetStorage();
		if (s != null) {
			storeCustomColumn(s, b);
			configuredProceed(SampleDetailScreen.key);
		}
	}
	
}
