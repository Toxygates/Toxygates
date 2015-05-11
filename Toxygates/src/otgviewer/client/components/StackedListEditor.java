package otgviewer.client.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import otgviewer.client.Utils;
import t.common.client.components.SetEditor;
import t.common.client.components.StringSelectionTable;
import t.common.shared.SharedUtils;
import t.viewer.shared.ItemList;
import t.viewer.shared.StringList;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.StackLayoutPanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Request;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * A StackedListEditor unifies multiple different methods of editing a list of strings.
 * Strings can be: compounds, genes, probes, ...
 *
 */
public class StackedListEditor extends ResizeComposite implements SetEditor<String> {

	private static Logger logger = SharedUtils.getLogger("sle");
	
	/**
	 * A selection method is a particular user interface for editing the list.
	 * It calls back to the StackedListEditor when the selection changes.
	 */
	public abstract static class SelectionMethod extends ResizeComposite {
		protected final StackedListEditor stackedEditor;		
		
		/**
		 * @param stackedEditor The editor that this selection method belongs to.
		 */
		public SelectionMethod(StackedListEditor stackedEditor) {
			this.stackedEditor = stackedEditor;
		} 
		
		/**
		 * Get the human-readable title of this selection method.
		 */
		public abstract String getTitle();
		
		/**
		 * Set the available items.
		 * @param items available items
		 * @param clearSelection whether the selection is to be cleared
		 * @param alreadySorted whether the items are sorted in order or not.
		 */
		public void setItems(List<String> items, boolean clearSelection, boolean alreadySorted) { }
		
		/**
		 * Set the currently selected items, reflecting the selection in the GUI.
		 * This should not cause changeSelection() to be called.
		 * The items should already have been validated.
		 * @param items
		 */
		public abstract void setSelection(Collection<String> items);		
	}
	
	/**
	 * A selection method that allows the user to edit a list as text, freely.
	 * Items are separated by commas or whitespace.
	 */
	public static class FreeEdit extends SelectionMethod {
		//TODO: the constants 10, 45 are somewhat ad-hoc -- find a better method in the future
		protected TextArea textArea = new ResizableTextArea(10, 45);
		private String lastText = "";
		private Timer t;
		private DockLayoutPanel dlp;
		private HorizontalPanel np;
		
		public FreeEdit(StackedListEditor editor) {
			super(editor);
			dlp = new DockLayoutPanel(Unit.PX);
			initWidget(dlp);
			
			Label l = new Label("Search:");			
			final SuggestBox sb = new SuggestBox(new SuggestOracle() {				
				@Override
				public void requestSuggestions(Request request, Callback callback) {
					callback.onSuggestionsReady(request, 
							new Response(stackedEditor.getSuggestions(request)));
				}
			});
			HorizontalPanel hp = Utils.mkHorizontalPanel(true, l, sb);					
			np = Utils.mkWidePanel();
			np.add(hp);
			
			sb.addSelectionHandler(new SelectionHandler<SuggestOracle.Suggestion>() {				
				@Override
				public void onSelection(SelectionEvent<Suggestion> event) {
					Suggestion s = event.getSelectedItem();
					String selection = s.getDisplayString();
					String oldText = textArea.getText().trim();
					String newText = (!"".equals(oldText)) ? (oldText + "\n" + selection) : selection;						
					textArea.setText(newText);			
					refreshItems(true);
					sb.setText("");
				}
			});
			
			dlp.addNorth(np, 36);
			textArea.setSize("100%", "100%");
			dlp.add(textArea);
			t = new Timer() {
				@Override
				public void run() {
					refreshItems(false);
				}				
			};			
			
			textArea.addKeyUpHandler(new KeyUpHandler() {				
				@Override
				public void onKeyUp(KeyUpEvent event) {
					lastText = textArea.getText();
					t.schedule(500);
				}				
			});
		}
		
		private void refreshItems(boolean immediate) {
			final FreeEdit fe = this;
			//Without the immediate flag, only do the refresh action if 
			// the text has been unchanged for 500 ms.
			if (immediate || lastText.equals(textArea.getText())) {
				String[] items = parseItems();
				Set<String> valid = stackedEditor.validateItems(Arrays.asList(items));
				if (!stackedEditor.getSelection().equals(valid)) {
					stackedEditor.setSelection(valid, fe);
				}
			}				
		}
		
		public String getTitle() {
			return "Edit/paste";
		}
		
		private String[] parseItems() {
			String s = textArea.getText();
			String[] split = s.split("\\s*[,\n]+\\s*");
			return split;
		}

		@Override
		public void setSelection(Collection<String> items) {
			textArea.setText(SharedUtils.mkString(items, "\n"));			
		}		
	}
	
	/**
	 * A selection method that allows the user to browse a list and check
	 * items with checkboxes.
	 * This is only recommended if the total number of available items is
	 * small (< 1000)
	 */
	public static class BrowseCheck extends SelectionMethod {
		private StringSelectionTable selTable;
		private DockLayoutPanel dlp = new DockLayoutPanel(Unit.PX);
		private Button sortButton;
		private ScrollPanel scrollPanel;
		
		private static Logger logger = SharedUtils.getLogger("sle.bc");
		
		public BrowseCheck(StackedListEditor editor, String itemTitle, 
				final int maxAutoSel) {
			super(editor);
			initWidget(dlp);
			
			final BrowseCheck bc = this;
			this.selTable = new StringSelectionTable("", itemTitle) {
				protected void selectionChanged(Set<String> selected) {
					bc.logger.info("Send selection " + selected.size());
					stackedEditor.setSelection(selected, bc);					
				}
			};			
			
			HorizontalPanel hp = Utils.mkWidePanel();		
			dlp.addSouth(hp, 36);
			
			sortButton = new Button("Sort by name", new ClickHandler() {
				public void onClick(ClickEvent ce) {
					List<String> items = new ArrayList<String>(stackedEditor.availableItems);
					Collections.sort(items);
					setItems(items, false, true);					
					sortButton.setEnabled(false);
				}
			});
			hp.add(sortButton);
			sortButton.setEnabled(false);
			
			hp.add(new Button("Select all", new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					List<String> items = selTable.getItems();
					List<String> sel = items;
					if (items.size() > maxAutoSel) {
						Window.alert("Too many items in list. Only the first " + maxAutoSel + " will be selected.");
						sel = items.subList(0, maxAutoSel);
					}
					
					setSelection(sel);
					stackedEditor.setSelection(sel, bc);
				}				
			}));
			
			hp.add(new Button("Unselect all", new ClickHandler() {
				public void onClick(ClickEvent ce) {
					List<String> empty = new ArrayList<String>();
					setSelection(empty);
					stackedEditor.setSelection(empty, bc);
				}
			}));
			scrollPanel = new ScrollPanel(selTable);
			dlp.add(scrollPanel);
		}	
		
		public String getTitle() {
			return "Browse";
		}

		@Override
		public void setSelection(Collection<String> items) {
			logger.info("Receive selection " + items.size());
			selTable.setSelection(items);
			selTable.table().redraw();
		}
		
		@Override
		public void setItems(List<String> items, boolean clearSelection, boolean alreadySorted) {
			selTable.setItems(items, clearSelection);			
			sortButton.setEnabled(!alreadySorted);			 
		}
		
		public void scrollToTop() {
			scrollPanel.scrollToTop();
		}
	}
	
	protected List<SelectionMethod> methods = new ArrayList<SelectionMethod>();
	protected Set<String> selectedItems = new HashSet<String>();
	protected Set<String> availableItems = new HashSet<String>();
	protected Map<String, String> caseCorrectItems = new HashMap<String, String>();
	protected Collection<StringList> predefinedLists;
	
	protected StringSelectionTable selTable = null;
	protected DockLayoutPanel dlp;
	protected StackLayoutPanel slp;

	protected VerticalPanel northVp;
	
	protected ListChooser listChooser;
	
	/**
	 * @param itemTitle Header for the item type being selected (in certain cases) 
	 * @param predefinedLists Predefined lists that the user may choose from 
	 */
	public StackedListEditor(final DataListenerWidget parent, String listType,
			String itemTitle,  int maxAutoSel,
			Collection<StringList> predefinedLists) {
		dlp = new DockLayoutPanel(Unit.PX);
		initWidget(dlp);
		
		this.predefinedLists = predefinedLists;
					
		northVp = Utils.mkVerticalPanel();
		northVp.setWidth("100%");

		final StackedListEditor sle = this;

		listChooser = new ListChooser(predefinedLists, listType) {
			@Override
			protected void itemsChanged(List<String> items) {
				setSelection(validateItems(items));
			}

			@Override
			protected void listsChanged(List<ItemList> itemLists) {
				parent.chosenItemLists = itemLists;
				sle.listsChanged(itemLists);
			}
		};		
		listChooser.setStylePrimaryName("colored");
		parent.addListener(listChooser);
		northVp.add(listChooser);
		
		dlp.addNorth(northVp, 30);
		
		slp = new StackLayoutPanel(Unit.PX);
		dlp.add(slp);

		createSelectionMethods(methods, itemTitle, maxAutoSel);
		for (SelectionMethod m: methods) {
			slp.add(m, m.getTitle(), 30);
		}
	}
	
	/**
	 * Instantiates the selection methods that are to be used.
	 * @param methods list to add methods to.
	 * @return
	 */
	protected void createSelectionMethods(List<SelectionMethod> methods, 
			String itemTitle, int maxAutoSel) {		
		methods.add(new FreeEdit(this));
		BrowseCheck bc = new BrowseCheck(this, itemTitle, maxAutoSel);
		methods.add(bc);
		this.selTable = bc.selTable; 
	}
	
	/**
	 * Obtain the inner string selection table, if it exists.
	 * May be null. 
	 * TODO: improve architecture
	 */
	@Nullable
	public StringSelectionTable selTable() {
		return selTable;
	}
	
	/**
	 * See above
	 */
	@Nullable
	public CellTable<String> table() {
		if (selTable != null) {
			return selTable.table();
		}
		return null;
	}	
	
	/**
	 * Validate items, some of which may have been entered manually.
	 * This method may be overridden for efficiency.
	 * @param items
	 * @return Valid items.
	 */
	protected Set<String> validateItems(List<String> items) {
		HashSet<String> r = new HashSet<String>();
		Iterator<String> i = items.iterator();
		String s = i.next();
		while(s != null) {			
			String v = validateItem(s);
			if (v != null) {
				r.add(v);
				if (i.hasNext()) { s = i.next(); } else { s = null; }
			} else {
				if (i.hasNext()) {
					String s2 = i.next();
					v = validateWithInfixes(s, s2);					
					if (v != null) {
						r.add(v);		
						if (i.hasNext()) { s = i.next(); } else { s = null; }
					} else {
						//Give up and treat s2 normally
						s = s2;
					}
				} else {
					s = null;
				}
			} 		
		} 
		return r;
	}
	
	private String validateWithInfixes(String s1, String s2) {
		//Some compounds have commas in their names but we also split compounds
		//on commas. 
		//E.g. 2,4-dinitrophenol and 'imatinib, methanesulfonate salt'
		//Test two together to get around this.
		final String[] infixes = new String[] { ",", ", " };
		for (String i: infixes) {
			String test = s1 + i + s2;
			String v = validateItem(test);
			if (v != null) {
				return v;
			}
		}
		return null;
	}

	/**
	 * Validate a single item.
	 * @param item
	 * @return The valid form (with case corrections etc) of the item.
	 */
	protected @Nullable String validateItem(String item) {
		String lower = item.toLowerCase();
		if (caseCorrectItems.containsKey(lower)) {
			return caseCorrectItems.get(lower);
		} else {
			return null;
		}
	}
	
	public Set<String> getSelection() {
		return selectedItems;
	}
	
	public void setItems(List<String> items, boolean clearSelection) {
		setItems(items, clearSelection, false);
	}
	
	/**
	 * Set the available items.
	 * @param items
	 * @return
	 */
	public void setItems(List<String> items, boolean clearSelection, boolean alreadySorted) {
		caseCorrectItems.clear();
		for (String i: items) {
			caseCorrectItems.put(i.toLowerCase(), i);
		}
		for (SelectionMethod m: methods) {
			m.setItems(items, clearSelection, alreadySorted);
		}		
		availableItems = new HashSet<String>(items);
	}
	
	public void setLists(List<ItemList> lists) {
		listChooser.setLists(lists);
	}
	
	/**
	 * Change the selection.
	 * @param items New selection
	 * @param from The selection method that triggered the change, or null 
	 * if the change was triggered externally. These items should already be
	 * validated.
	 */
	protected void setSelection(Collection<String> items, 
			@Nullable SelectionMethod from) {		
		logger.info("Receive selection " + items.size() + " from " +
			(from != null ? from.getClass().toString() : "null"));
		for (SelectionMethod m: methods) {
			if (m != from) {
				m.setSelection(items);
			}
		}
		selectedItems = new HashSet<String>(items);
		listChooser.setItems(new ArrayList<String>(items));
		selectionChanged(selectedItems);
	}
	
	public void setSelection(Collection<String> items) {
		setSelection(items, null);		
	}
	
	/**
	 * Outgoing signal. Called when the selection has changed.
	 * @param items
	 */
	protected void selectionChanged(Set<String> items) {}
	
	/**
	 * Ditto
	 * @param itemLists
	 */
	protected void listsChanged(List<ItemList> itemLists) {}
	
	public void clearSelection() {
		setSelection(new HashSet<String>());
	}
	
	/**
	 * Display the picker method, if one exists.
	 */
	public void displayPicker() {
		for (SelectionMethod m: methods) {
			if (m instanceof BrowseCheck) {
				slp.showWidget(m);
				((BrowseCheck) m).scrollToTop();
				return;
			}
		}
		//Should not get here!
		Window.alert("Technical error: no such selection method in StackedListEditor");
	}	
	
	protected List<Suggestion> getSuggestions(Request request) {
		String lc = request.getQuery().toLowerCase();
		List<Suggestion> r =  new ArrayList<Suggestion>();
		for (String k : caseCorrectItems.keySet()) {
			if (k.startsWith(lc)) {
				final String suggest = caseCorrectItems.get(k);
				r.add(new Suggestion() {					
					@Override
					public String getReplacementString() {
						return suggest;
					}
					
					@Override
					public String getDisplayString() {
						return suggest;
					}
				});
			}
		}
		return r;
	}	
}