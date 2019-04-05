// "use strict";
/* identifiers for cystoscape display panels */
const MAIN_ID = 0; // left-side
const SIDE_ID = 1; // right-side
const BOTH_ID = 2; // both panels, used for intersection

/* these are the main and side graphs - as Cytoscape objects */
var vizNet = [null, null];

/**
 * Initialize the main visualization display.
 * Method called by Toxygates to start the visualization of a graph. A single
 * panel is added to applications DOM and a cytoscape object is associated to it
 */
function onReadyForVisualization(){
  /* only add the required structures for pop-up display, if they are not part
   * of the DOM already */
  if( $('#nodePopper').length === 0 ){
    addPopperDiv();
  }
  /* if they are, then simply put them at the bottom of the tree for proper
   * display */
  else{
    $('body').append($('#nodePopper'));
  }

  /* capture the parent node for the visualization panel */
  $("#display")
    /* add a new panel to display a graph */
    .append('<div id="leftDisplay" class="sub-viz"></div>')
    .ready(function(){
      let left = $("#leftDisplay");
      /* set a data field with the id of the element */
      left.data("idx", MAIN_ID);
      /* add a new cytoscape element to the <div> */
      initCytoscapeGraph(MAIN_ID, left);
    })
    /* Append modal dialogs to the visualization panel in the DOM */
    .append($('#colorScaleDialog'))
    .append($('#changeLabelDialog'))
    /* Listener to hide pop-ups when leaving the display area */
    .on('mouseout', function(){
      $('#nodePopper').css('display', 'none');
    })
  ;
}

/**
 * Initialize secondary visualization display.
 * Enable a dual panel visualization, by adding an extra DOM component. The
 * extra component is only added once, so we need to double check that the panel
 * is not already there, before creating it.
 */
function showNetworkOnRight() {
  /* set the interface required for dual panel visualization */
  setDualPanelInterface();
  /* Check if there is already a right panel */
  let right = $("#rightDisplay");
  if( right.length !== 0 ){
    initCytoscapeGraph(SIDE_ID, right);
    return;
  }
  /* Have the left-panel reduce its size to half of the available display */
  $("#leftDisplay").addClass("with-side");
  /* Add a new panel for the display of a second graph */
  $("#display")
    .append('<div id="rightDisplay" class="sub-viz"></div>')
    .ready(function(){
      let right = $("#rightDisplay");
      /* set a data field with the id of the element */
      right.data('idx', SIDE_ID);
      /* add a new cytoscape element to the container */
      initCytoscapeGraph(SIDE_ID, right);
      /* Fit the left graph to the smaller viewport */
      vizNet[MAIN_ID].resize();
      vizNet[MAIN_ID].fit();
    })
    ;
}

/**
 * Initialize a cytoscape object
 * Init a new instance of cytoscape graph with default display properties for
 * nodes and edges, and associates it with the specified DOM container.
 * Each cytoscape is also associated listeners to handle the display of pop-up
 * information and context menus.
 *
 * @type {HTMLElement}
 * @param {Number} id The id of the display panel being initialized.
 * @param {HTMLElement} container The DOM element (usually a <div>) that will be
 * used as container for a cytoscape element.
 */
function initCytoscapeGraph(id, container){
  /* init the object as a cytoscape instance, and associate it to the provided
   * container */
  vizNet[id] = cytoscape({ container: container });
  /* set default style for nodes and edges in the network */
  vizNet[id].initStyle();
  /* bind functions used to handle the display and hiding of pop-ups */
  vizNet[id].on("mouseover", "node", onNodeEnter);
  vizNet[id].on("mouseout", "node", onNodeExit);
  /* add behaviour to the selection and unselections of nodes */
  vizNet[id].on("select", "node", onNodeSelection);
  vizNet[id].on("unselect", "node", onNodeUnselection);

  /* add a context menu to the panel (delete the previous one if still there),
   * and position it within the DOM at a level where events will be captured */
  if ( $('.ctx-menu-'+id).length !== 0 ){
    $('.ctx-menu-'+id).remove();
  }
  vizNet[id].initContextMenu(id);
  $(".ctx-menu-"+id)
    .appendTo($(".gwt-DialogBox")[0])
    .hide();

  /* load the graph elements and add them to the display */
  let positioned = changeNetwork(id);

  /* Update the value of panel select */
  $('#panelSelect').val(id);

  /* Update the layout select. If the loaded graph had no positioning, apply a
   * concentric layout by default */
  let lyt = positioned ? vizNet[id].options().layout['name'] : 'concentric';
  $('#layoutSelect')
    .val(lyt)
    .trigger('change');

  /* Update the display hidden nodes checkbox */
  $('#showHiddenNodesCheckbox').prop('checked', false);
  vizNet[id].options().layout['showHidden'] = false;
}

/**
 * Load graph data provided by toxygates into a Cytoscape instance
 * Data being currently inspected by the user through the table view of
 * Toxygates is provided for visualization through the 'convertedNetwork'
 * variable.
 * Here, data is provided as JSON style dictionaries, containing a list of nodes
 * and interactions, that can be used to populate the Cytoscape instance of the
 * graph used for visualization.
 * This function is also called whenever a change is made to the network
 * elements (ex. via filtering) in order to update the corresponding
 * visualization.
 *
 * @param {number} id Identifies the display to which changes should be made
 */
function changeNetwork(id=MAIN_ID){
  /* remove all previous data */
  vizNet[id].elements().remove();
  /* store the name of the graph as data field in the container */
  vizNet[id].options().container.data('title', convertedNetwork.title);
  /* load the content of 'convertedNetwork' as elements for the graph */
  let positioned = vizNet[id].loadElements(convertedNetwork);
  /* mark unconnected nodes as hidden */
  vizNet[id].hideUnconnected();
  /* fit the graph to the current viewport */
  vizNet[id].fit();
  /* return if the elements had a position or not */
  return positioned;
}

/**
 * Identify the panel currently being manipulated by the user.
 * Show the appropriate layout depending on the panel selected by the user. If
 * both panels are selected, a custom (do not modify the current positions)
 * layout is selected.
 * Show the appropriate hiding of hidden nodes. If both panels are selected, the
 * hidden nodes are hidden, and the corresponding event is triggered.
 */
$(document).on("change", "#panelSelect", function(){
  /* Capture the panel selected by the user */
  let id = parseInt($("#panelSelect").val());
  switch(id){
    /* single panel selection */
    case MAIN_ID:
    case SIDE_ID:
      /* Display the appropriate layout */
      let opt = vizNet[id].options();
      $("#layoutSelect").val(opt.layout['name']);
      /* Display the appropriate hiding of nodes */
      $('#showHiddenNodesCheckbox').prop('checked', opt.layout['showHidden']);
      break;
    /* dual panel selection */
    case BOTH_ID:
      /* Default layout set to None */
      $("#layoutSelect").val('custom');
      /* Default display of hidden nodes to False */
      $('#showHiddenNodesCheckbox').prop('checked', false);
      $('#showHiddenNodesCheckbox').trigger('change');
      break;
  }
});

/**
 * Apply the selected layout to the active graph(s).
 * Apply the layout identified with the corresponding name, using default values
 * for all options. In the case of 'custom' layout, the positions of the nodes
 * are not modified (i.e. no automatic layout is applied).
 * When a single layout is to be applied to two netorks, then the layout is only
 * applied to the intersecting nodes on both networks, and a grid layout is
 * applied to the remainder nodes on both networks.
 */
$(document).on("change", "#layoutSelect", function (){
  /* Capture the panel selected by the user */
  let id = parseInt($("#panelSelect").val());
  /* Retrieve the name of the selected layout */
  let name = $("#layoutSelect").find(":selected").val();
  /* We only apply the selected layout to the graphs currently selected as
   * active, by the id indicator (could be both) */
  if( name !== 'custom'){
    switch(id){
      /* Apply layout to all nodes in a single panel */
      case MAIN_ID:
      case SIDE_ID:
        vizNet[id].elements(':visible').updateLayout(name).run();
        vizNet[id].options().layout.name = name;
        break;
      /* Apply layout to intersecting nodes in both panels */
      case BOTH_ID:
        /* Selected layout is applied to intersecting nodes, whilst a
         * complementary layout is applied to other nodes in both panels */
        vizNet[MAIN_ID].dualLayout(vizNet[SIDE_ID], name, 'grid');
        /* refit the side panel, as nodes could have been positioned outside the
         * viewing area (based on the world positions of the main panel) */
        vizNet[SIDE_ID].fit();
        /* set the layout for both networks as a default 'None' */
        vizNet[MAIN_ID].options().layout.name = 'custom';
        vizNet[SIDE_ID].options().layout.name = 'custom';
        break;
    }
  }
});

/**
 * Hide/show nodes classed as 'Hidden'.
 * All unconnected nodes (by default) and user selected nodes classified as
 * 'hidden' are, as the name suggests, removed from the visualization and not
 * considered when laying out elements.
 * These nodes can be shown by checking the corresponding checkbox.
 */
$(document).on("change", "#showHiddenNodesCheckbox", function(){
  /* Capture the panel selected by the user */
  let id = parseInt($("#panelSelect").val());
  /* Determine if the hidden nodes should be shown or not */
  let showHidden = $("#showHiddenNodesCheckbox").is(":checked");
  /* show/hide the hidden nodes in the corresponding panels */
  switch(id){
    case MAIN_ID:
    case SIDE_ID:
      vizNet[id].toggleHiddenNodes(showHidden);
      vizNet[id].fit();
      break;
    case BOTH_ID:
      vizNet[MAIN_ID].toggleHiddenNodes(showHidden);
      vizNet[MAIN_ID].fit();
      vizNet[SIDE_ID].toggleHiddenNodes(showHidden);
      vizNet[SIDE_ID].fit();
      break;
  }
  /* trigger the change in layout, as the network potentially changed */
  $("#layoutSelect").trigger("change");
});

/**
 * Highlight the intersection between networks.
 * Only when visualizing two networks simultaneously, the user can choose to
 * highlight the intersecting elements between both structures.
 */
$(document).on("change", "#showIntersectionCheckbox", function(){
  let hlgh = $("#showIntersectionCheckbox").is(":checked");
  vizNet[MAIN_ID].toggleIntersectionHighlight(vizNet[SIDE_ID], hlgh);
});

/**
 * Merge into a single structure both currently displayed networks.
 * Only when visualizing two networks simultaneously, the user is able to merge
 * them into a single structure. This resulting structure is visualized using
 * a single display panel.
 */
$(document).on("click", "#mergeNetworkButton", function(){
  /* Remove DOM elements for the right SIDE_ID */
  removeRightDisplay();
  /* Perform the merge of the networks */
  vizNet[MAIN_ID].mergeWith(vizNet[SIDE_ID].elements());
  /* Set a default custom layout for the merged network and update the select
   * component accordingly */
  vizNet[MAIN_ID].options().layout['name'] = 'custom';
  $("#layoutSelect").val(vizNet[MAIN_ID].options().layout['name']);
  /* Set a default hiding of hidden nodes and update the checkbox accordingly */
  let showHidden = $("#showHiddenNodesCheckbox").prop("checked", false);
  $("#showHiddenNodesCheckbox").trigger("change");
  /* Clean un-required variables */
  vizNet[SIDE_ID] = null;
  /* Fit the new network to the viewport */
  vizNet[MAIN_ID].resize();
  vizNet[MAIN_ID].fit();
});

/**
 * Close the right visualization panel
 */
$(document).on('click', '#closeRightPanelButton', function(){
  /* Remove DOM elements for the right panel SIDE_ID */
  removeRightDisplay();
  /* Fit the remaining network to the whole available space */
  vizNet[MAIN_ID].resize();
  vizNet[MAIN_ID].fit();
});

/**
 * Called by Toxygates to get the desired height, in pixels, of the user
 * interaction div
 */
function uiHeight(){
  return 92;
}

/**
 * Updates toxyNet with the changes made to vizNet
 */
function updateToxyNet(){
  var title = toxyNet["title"];
  var nodes = vizNet[MAIN_ID].getToxyNodes();
  var edges = vizNet[MAIN_ID].getToxyInteractions();

  toxyNet[MAIN_ID] = new Network(title, edges, nodes);
}

/**   FUNCTIONS TO HANDLE USER INTERACTION WITH MODAL DIALOGS        **/

/**
 * Apply a color scale to a network
 * Once the user has selected the relevant options from the corresponding dialog,
 * the color coming from a linear transformation of white to color is used to
 * update the display of all nodes in the network.
 */
$(document).on("click", "#okColorScaleDialog", function (event){
  /* identify the graph on to appply the color scale */
  let id  = $("#colorScaleDialog").data('id');

  /* retrieve the selected weights for both msgRNA and microRNA */
  let msgWgt = $("#msgRNAWeight").val();
  let micWgt = $('#microRNAWeight').val();
  /* retrieve colors used for negative and positive values of the scale */
  let negColor = $("#negColor").val();
  let posColor = $('#posColor').val();
  /* retrieve cap values for negative and positive ends of the scale */
  let min = $('#negCap').val();
  let max = $('#posCap').val();
  /* apply the linearly interpolated color to each node in the graph */
  vizNet[id].nodes().forEach(function(ele){
    /* retrieve the current node's weight value */
    let val = ele.data('weight')[msgWgt];
    if (ele.data('type') === nodeType.microRNA )
      val = ele.data('weight')[micWgt];

    /* calculate the color for the current node */
    let c = valueToColor(val, min, max, negColor, posColor);
    /* if the color is valid, assign it to the node */
    if( c !== undefined ){
      ele.data("color", c);
      val <= 0 ? ele.data('borderColor', negColor) : ele.data('borderColor', posColor);
    }
    /* else, revert the node to default color */
    else
      ele.setDefaultStyle();
  });
  /* hide the modal after color has been applied to nodes */
  $("#colorScaleDialog").css('visibility', 'hidden');
});

/**
 * Change the label of a node
 * Use the user defined string as label for the selected node and include in the
 * display.
 */
$(document).on("click", "#okChangeLabelDialog", function (event){
  /* identify the graph on to appply the color scale */
  let id  = $("#changeLabelDialog").data('id');
  let nodeid = '#'+$('#changeLabelDialog').data('nodeid');

  /* retrieve the selected label from the user */
  let label = $("#nodeLabel").val();

  label = label.replace(/([^a-z0-9\-]+)/gi, '-');
  // ^[a-zA-Z0-9._-]+$/

  /* apply the new label to the corresponding node in the graph */
  vizNet[id].nodes(nodeid).data('label', label);

  /* hide the modal after color has been applied to nodes */
  $("#changeLabelDialog").css('visibility', 'hidden');
});

/**
 * Hide a modal dialog.
 * the corresponding modal when the close option is selected.
 * No changes are applied and whatever information the user added to the modal
 * components is lost.
 */
$(document).on("click", ".cancelBtn", function(event){
  let dialog = $(event.target).data().dialog;
  $("#"+dialog).css('visibility', 'hidden');
});
