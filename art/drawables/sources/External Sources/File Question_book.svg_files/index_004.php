// <source lang="javascript">

/*
  Cross-browser tooltip support for MediaWiki.
  
  Author: [[User:Lupo]], March 2008
  License: Quadruple licensed GFDL, GPL, LGPL and Creative Commons Attribution 3.0 (CC-BY-3.0)
  
  Choose whichever license of these you like best :-)

  Based on ideas gleaned from prototype.js and prototip.js.
  http://www.prototypejs.org/
  http://www.nickstakenburg.com/projects/prototip/
  However, since prototype is pretty large, and prototip had some
  problems in my tests, this stand-alone version was written.
  
  Note: The fancy effects from scriptaculous have not been rebuilt.
  http://script.aculo.us/
  
  See http://commons.wikimedia.org/wiki/MediaWiki_talk:Tooltips.js for
  more information including documentation and examples.
*/

var is_IE       = !!window.ActiveXObject;
var is_IE_pre_7 = is_IE 
               && (function(agent) {
                     var version = new RegExp('MSIE ([\\d.]+)').exec(agent);
                     return version ? (parseFloat(version[1]) < 7) : false;
                   })(navigator.userAgent);

var EvtHandler = {
  listen_to : function (object, node, evt, f)
  {
    var listener = EvtHandler.make_listener (object, f);
    EvtHandler.attach (node, evt, listener);
  },
  
  attach : function (node, evt, f)
  {
    if (node.attachEvent) node.attachEvent ('on' + evt, f);
    else if (node.addEventListener) node.addEventListener (evt, f, false);
    else node['on' + evt] = f;
  },
  
  remove : function (node, evt, f)
  {
    if (node.detachEvent) node.detachEvent ('on' + evt, f);
    else if (node.removeEventListener) node.removeEventListener (evt, f, false);
    else node['on' + evt] = null;
  },
  
  make_listener : function (obj, listener)
  {
    // Some hacking around to make sure 'this' is set correctly
    var object = obj, f = listener;
    return function (evt) { return f.apply (object, [evt || window.event]); }
  },

  mouse_offset : function ()
  {
    // IE does some strange things...
    // This is adapted from dojo 0.9.0, see http://dojotoolkit.org
    if (is_IE) {
      var doc_elem = document.documentElement;
      if (doc_elem) {
        if (typeof (doc_elem.getBoundingClientRect) == 'function') {
          var tmp = doc_elem.getBoundingClientRect ();
          return {x : tmp.left, y : tmp.top};
        } else {
          return {x : doc_elem.clientLeft, y : doc_elem.clientTop};
        }
      }
    }
    return null;
  },
  
  killEvt : function (evt)
  {
    if (typeof (evt.preventDefault) == 'function') {
      evt.stopPropagation ();
      evt.preventDefault (); // Don't follow the link
    } else if (typeof (evt.cancelBubble) != 'undefined') { // IE...
      evt.cancelBubble = true;
    }
    return false; // Don't follow the link (IE)
  }

} // end EvtHandler

var Buttons = {
  
  buttonClasses : {},
  
  createCSS : function (imgs, sep, id)
  {
    var width   = imgs[0].getAttribute ('width');
    var height  = imgs[0].getAttribute ('height');
    try {
      // The only way to set the :hover and :active properties through Javascript is by
      // injecting a piece of CSS. There is no direct access within JS to these properties.
      var sel1  = "a" + sep + id;
      var prop1 = "border:0; text-decoration:none; background-color:transparent; "
                + "width:" + width + "px; height:" + height + "px; "
                + (is_gecko ? "display:-moz-inline-box; vertical-align:middle; "
                            : "display:inline-block; ")
                + "background-position:left; background-repeat:no-repeat; "
                + "background-image:url(" + imgs[0].src + ");";
      var sel2  = null, prop2 = null, sel3  = null, prop3 = null; // For IE...
      var css   = sel1 + ' {' + prop1 + '}\n';                    // For real browsers
      if (imgs.length > 1 && imgs[1]) {
        sel2  = "a" + sep + id + ":hover";
        prop2 = "background-image:url(" + imgs[1].src + ");";
        css   = css + sel2 + ' {' + prop2 + '}\n';
      }
      if (imgs.length > 2 && imgs[2]) {
        sel3  = "a" + sep + id + ":active"
        prop3 = "background-image:url(" + imgs[2].src + ");";
        css   = css + sel3 + ' {' + prop3 + '}\n';
      }
      // Now insert a style sheet with these properties into the document (or rather, its head).
      var styleElem = document.createElement( 'style' );
      styleElem.setAttribute ('type', 'text/css');
      try {
        styleElem.appendChild (document.createTextNode (css));
        document.getElementsByTagName ('head')[0].appendChild (styleElem);
      } catch (ie_bug) {
        // Oh boy, IE has a big problem here
        document.getElementsByTagName ('head')[0].appendChild (styleElem);
//        try {
          styleElem.styleSheet.cssText = css;
/*
        } catch (anything) {
          if (document.styleSheets) {
            var lastSheet = document.styleSheets[document.styleSheets.length - 1];          
            if (lastSheet && typeof (lastSheet.addRule) != 'undefined') {
              lastSheet.addRule (sel1, prop1);
              if (sel2) lastSheet.addRule (sel2, prop2);
              if (sel3) lastSheet.addRule (sel3, prop3);
            }
          }
        }
*/
      }
    } catch (ex) {
      return null;
    }
    if (sep == '.') {
      // It's a class: remember the first image
      Buttons.buttonClasses[id] = imgs[0];
    }
    return id;
  }, // end createCSS
  
  createClass : function (imgs, id)
  {
    return Buttons.createCSS (imgs, '.', id);
  },
  
  makeButton : function (imgs, id, handler, title)
  {
    var success     = false;
    var buttonClass = null;
    var content     = null;
    if (typeof (imgs) == 'string') {
      buttonClass = imgs;
      content     = Buttons.buttonClasses[imgs];
      success     = (content != null);
    } else {
      success     = (Buttons.createCSS (imgs, '#', id) != null);
      content     = imgs[0];
    }
    if (success) {
      var lk = document.createElement ('a');
      lk.setAttribute
        ('title', title || content.getAttribute ('alt') || content.getAttribute ('title') || "");
      lk.id = id;
      if (buttonClass) lk.className = buttonClass;
      if (typeof (handler) == 'string') {
        lk.href = handler;
      } else {
        lk.href = '#'; // Dummy, overridden by the onclick handler below.
        lk.onclick = function (evt)
          {
            var e = evt || window.event; // W3C, IE
            try {handler (e);} catch (ex) {};
            return EvtHandler.killEvt (e);
          };
      }
      content = content.cloneNode (true);
      content.style.visibility = 'hidden';
      lk.appendChild (content);
      return lk;
    } else {
      return null;
    }
  } // end makeButton
  
} // end Button

var Tooltips = {
  // Helper object to force quick closure of a tooltip if another one shows up.
  debug    : false,  
  top_tip  : null,
  nof_tips : 0,

  new_id : function ()
  {
    Tooltips.nof_tips++;
    return 'tooltip_' + Tooltips.nof_tips;
  },

  register : function (new_tip)
  {
    if (Tooltips.top_tip && Tooltips.top_tip != new_tip) Tooltips.top_tip.hide_now ();
    Tooltips.top_tip = new_tip;
  },
  
  deregister : function (tip)
  {
    if (Tooltips.top_tip == tip) Tooltips.top_tip = null;
  },

  close : function ()
  {
    if (Tooltips.top_tip) {
      Tooltips.top_tip.hide_now ();
      Tooltips.top_tip = null;
    }
  }
}

var Tooltip = function () {this.initialize.apply (this, arguments);}
// This is the Javascript way of creating a class. Methods are added below to Tooltip.prototype;
// one such method is 'initialize', and that will be called when a new instance is created.
// To create instances of this class, use var t = new Tooltip (...);

// Location constants
Tooltip.MOUSE             = 0; // Near the mouse pointer
Tooltip.TRACK             = 1; // Move tooltip when mouse pointer moves
Tooltip.FIXED             = 2; // Always use a fixed poition (anchor) relative to an element

// Anchors
Tooltip.TOP_LEFT          = 1;
Tooltip.TOP_RIGHT         = 2;
Tooltip.BOTTOM_LEFT       = 3;
Tooltip.BOTTOM_RIGHT      = 4;

// Activation constants
Tooltip.NONE              = -1; // You must show the tooltip explicitly in this case.
Tooltip.HOVER             =  1;
Tooltip.FOCUS             =  2; // always uses the FIXED position
Tooltip.CLICK             =  4;
Tooltip.ALL_ACTIVATIONS   =  7;

// Deactivation constants

Tooltip.MOUSE_LEAVE       =  1; // Mouse leaves target, alternate target, and tooltip
Tooltip.LOSE_FOCUS        =  2; // Focus changes away from target
Tooltip.CLICK_ELEM        =  4; // Target is clicked
Tooltip.CLICK_TIP         =  8; // Makes only sense if not tracked
Tooltip.ESCAPE            = 16;
Tooltip.ALL_DEACTIVATIONS = 31;
Tooltip.LEAVE             = Tooltip.MOUSE_LEAVE | Tooltip.LOSE_FOCUS;

// On IE, use the mouseleave/mouseenter events, which fire only when the boundaries of the
// element are left (but not when the element if left because the mouse moved over some
// contained element)

Tooltip.mouse_in    = (is_IE ? 'mouseenter' : 'mouseover');
Tooltip.mouse_out   = (is_IE ? 'mouseleave' : 'mouseout');

Tooltip.prototype =
{
  initialize : function (on_element, tt_content, opt, css)
  {
    if (!on_element || !tt_content) return;
    this.tip_id      = Tooltips.new_id ();
    // Registering event handlers via attacheEvent on IE is apparently a time-consuming
    // operation. When you add many tooltips to a page, this can add up to a noticeable delay.
    // We try to mitigate that by only adding those handlers we absolutely need when the tooltip
    // is created: those for showing the tooltip. The ones for hiding it again are added the
    // first time the tooltip is actually shown. We thus record which handlers are installed to
    // avoid installing them multiple times:
    //   event_state: -1 : nothing set, 0: activation set, 1: all set
    //   tracks:      true iff there is a mousemove handler for tracking installed.
    // This change bought us about half a second on IE (for 13 tooltips on one page). On FF, it
    // doesn't matter at all; in Firefoy, addEventListener is fast anyway.
    this.event_state = -1;
    this.tracks      = false;
    // We clone the node, wrap it, and re-add it at the very end of the
    // document to make sure we're not within some nested container with
    // position='relative', as this screws up all absolute positioning
    // (We always position in global document coordinates.)
    // In my tests, it appeared as if Nick Stakenburg's "prototip" has
    // this problem...
    if (typeof (tt_content) == 'function') {
      this.tip_creator = tt_content;
      this.css         = css;
      this.content     = null;
    } else {
      this.tip_creator = null;
      this.css         = null;
      if (tt_content.parentNode) {
        if (tt_content.ownerDocument != document)
          tt_content = document.importNode (tt_content, true);
        else
          tt_content = tt_content.cloneNode (true);
      }
      tt_content.id = this.tip_id;
      this.content  = tt_content;
    }
    // Wrap it
    var wrapper = document.createElement ('div');
    wrapper.className = 'tooltipContent';
    // On IE, 'relative' triggers lots of float:right bugs (floats become invisible or are
    // mispositioned).
    //if (!is_IE) wrapper.style.position = 'relative';
    if (this.content) wrapper.appendChild (this.content);
    this.popup = document.createElement ('div');
    this.popup.style.display = 'none';
    this.popup.style.position = 'absolute';
    this.popup.style.top = "0px";
    this.popup.style.left = "0px";
    this.popup.appendChild (wrapper);
    // Set the options
    this.options = {
       mode         : Tooltip.TRACK              // Where to display the tooltip.
      ,activate     : Tooltip.HOVER              // When to activate
      ,deactivate   : Tooltip.LEAVE | Tooltip.CLICK_ELEM | Tooltip.ESCAPE // When to deactivate
      ,mouse_offset : {x: 5, y: 5, dx: 1, dy: 1} // Pixel offsets and direction from mouse pointer
      ,fixed_offset : {x:10, y: 5, dx: 1, dy: 1} // Pixel offsets from anchor position
      ,anchor       : Tooltip.BOTTOM_LEFT        // Anchor for fixed position
      ,target       : null                       // Optional alternate target for fixed display.
      ,max_width    :    0.6         // Percent of window width (1.0 == 100%)
      ,max_pixels   :    0           // If > 0, maximum width in pixels
      ,z_index      : 1000           // On top of everything
      ,open_delay   :  500           // Millisecs, set to zero to open immediately
      ,hide_delay   : 1000           // Millisecs, set to zero to close immediately
      ,close_button : null           // Either a single image, or an array of up to three images
                                     // for the normal, hover, and active states, in that order
      ,onclose      : null           // Callback to be called when the tooltip is hidden. Should be
                                     // a function taking a single argument 'this' (this Tooltip)
                                     // an an optional second argument, the event.
      ,onopen       : null           // Ditto, called after opening.
    };
    // The lower of max_width and max_pixels limits the tooltip's width.
    if (opt) { // Merge in the options
      for (var option in opt) {
        if (option == 'mouse_offset' || option == 'fixed_offset') {
          try {
            for (var attr in opt[option]) {
              this.options[option][attr] = opt[option][attr];
            }
          } catch (ex) {
          }
        } else
          this.options[option] = opt[option];
      }
    }
    // Set up event handlers as appropriate
    this.eventShow   = EvtHandler.make_listener (this, this.show);
    this.eventToggle = EvtHandler.make_listener (this, this.toggle);
    this.eventFocus  = EvtHandler.make_listener (this, this.show_focus);
    this.eventClick  = EvtHandler.make_listener (this, this.show_click);
    this.eventHide   = EvtHandler.make_listener (this, this.hide);
    this.eventTrack  = EvtHandler.make_listener (this, this.track);
    this.eventClose  = EvtHandler.make_listener (this, this.hide_now);
    this.eventKey    = EvtHandler.make_listener (this, this.key_handler);

    this.close_button       = null;
    this.close_button_width = 0;
    if (this.options.close_button) {
      this.makeCloseButton ();
      if (this.close_button) {
        // Only a click on the close button will close the tip.
        this.options.deactivate = this.options.deactivate & ~Tooltip.CLICK_TIP;
        // And escape is always active if we have a close button
        this.options.deactivate = this.options.deactivate | Tooltip.ESCAPE;
        // Don't track, you'd have troubles ever getting to the close button.
        if (this.options.mode == Tooltip.TRACK) this.options.mode = Tooltip.MOUSE;
        this.has_links = true;
      }
    }
    if (this.options.activate == Tooltip.NONE) {
      this.options.activate = 0;
    } else {
      if ((this.options.activate & Tooltip.ALL_ACTIVATIONS) == 0) {
        if (on_element.nodeName.toLowerCase () == 'a')
          this.options.activate = Tooltip.CLICK;
        else
          this.options.activate = Tooltip.HOVER;
      }
    }
    if ((this.options.deactivate & Tooltip.ALL_DEACTIVATIONS) == 0 && !this.close_button)
      this.options.deactivate = Tooltip.LEAVE | Tooltip.CLICK_ELEM | Tooltip.ESCAPE;
    document.body.appendChild (this.popup);
    if (this.content) this.apply_styles (this.content, css); // After adding it to the document
    // Clickable links?
    if (this.content && this.options.mode == Tooltip.TRACK) {
      this.setHasLinks ();
      if (this.has_links) {
        // If you track a tooltip with links, you'll never be able to click the links
        this.options.mode = Tooltip.MOUSE;
      }
    }
    // No further option checks. If nonsense is passed, you'll get nonsense or an exception.
    this.popup.style.zIndex = "" + this.options.z_index;
    this.target             = on_element;
    this.open_timeout_id    = null;
    this.hide_timeout_id    = null;
    this.size               = {width : 0, height : 0};
    this.setupEvents (EvtHandler.attach, 0);
    this.ieFix = null;
    if (is_IE) {
      // Display an invisible IFrame of the same size as the popup beneath it to make popups
      // correctly cover "windowed controls" such as form input fields in IE. For IE >=5.5, but
      // who still uses older IEs?? The technique is also known as a "shim". A good
      // description is at http://dev2dev.bea.com/lpt/a/39
      this.ieFix = document.createElement ('iframe');
      this.ieFix.style.position = 'absolute';
      this.ieFix.style.border   = '0';
      this.ieFix.style.margin   = '0';
      this.ieFix.style.padding  = '0';
      this.ieFix.style.zIndex   = "" + (this.options.z_index - 1); // Below the popup
      this.ieFix.tabIndex       = -1;
      this.ieFix.frameBorder    = '0';
      this.ieFix.style.display  = 'none';
      document.body.appendChild (this.ieFix);
      this.ieFix.style.filter  = 'alpha(Opacity=0)'; // Ensure transparency
    } 
  },
  
  apply_styles : function (node, css)
  {
    if (css) {
      for (var styledef in css) node.style[styledef] = css[styledef];
    }
    if (is_opera_95 || this.close_button) node.style.opacity = "1.0"; // Bug workaround.
    // FF doesn't handle the close button at all if it is (partially) transparent...
    if (node.style.display == 'none') node.style.display = "";
  },

  setHasLinks : function ()
  {
    if (this.close_button) { this.has_links = true; return; }
    var lks = this.content.getElementsByTagName ('a');
    this.has_links = false;
    for (var i=0; i < lks.length; i++) {
      var href = lks[i].getAttribute ('href');
      if (href && href.length > 0) { this.has_links = true; return; }
    }
    // Check for form elements
    function check_for (within, names)
    {
      if (names) {
        for (var i=0; i < names.length; i++) {
          var elems = within.getElementsByTagName (names[i]);
          if (elems && elems.length > 0) return true;
        }
      }
      return false;
    }
    this.has_links = check_for (this.content, ['form', 'textarea', 'input', 'button', 'select']);
  },

  setupEvents : function (op, state)
  {
    if (state < 0 || state == 0 && this.event_state < state) {
      if (this.options.activate & Tooltip.HOVER)
        op (this.target, Tooltip.mouse_in, this.eventShow);
      if (this.options.activate & Tooltip.FOCUS)
        op (this.target, 'focus', this.eventFocus);
      if (   (this.options.activate & Tooltip.CLICK)
          && (this.options.deactivate & Tooltip.CLICK_ELEM)) {
        op (this.target, 'click', this.eventToggle);
      } else {
        if (this.options.activate & Tooltip.CLICK)
          op (this.target, 'click', this.eventClick);
        if (this.options.deactivate & Tooltip.CLICK_ELEM)
          op (this.target, 'click', this.eventClose);
      }
      this.event_state = state;
    }
    if (state < 0 || state == 1 && this.event_state < state) {
      if (this.options.deactivate & Tooltip.MOUSE_LEAVE) {
        op (this.target, Tooltip.mouse_out, this.eventHide);
        op (this.popup, Tooltip.mouse_out, this.eventHide);
        if (this.options.target) op (this.options.target, Tooltip.mouse_out, this.eventHide);
      }
      if (this.options.deactivate & Tooltip.LOSE_FOCUS)
        op (this.target, 'blur', this.eventHide);
      if (   (this.options.deactivate & Tooltip.CLICK_TIP)
          && (this.options.mode != Tooltip.TRACK))
        op (this.popup, 'click', this.eventClose);        
      
      // Some more event handling
      if (this.hide_delay > 0) {
        if (!(this.options.activate & Tooltip.HOVER))
          op (this.popup, Tooltip.mouse_in, this.eventShow);
        op (this.popup, 'mousemove', this.eventShow);
      }
      this.event_state = state;
    }
    if (state < 0 && this.tracks)
      op (this.target, 'mousemove', this.eventTrack);
  },
  
  remove: function ()
  {
    this.hide_now ();
    this.setupEvents (EvtHandler.remove, -1);
    this.tip_creator = null;
    document.body.removeElement (this.popup);
    if (this.ieFix) document.body.removeElement (this.ieFix);
  },
  
  show : function (evt)
  {
    this.show_tip (evt, true);
  },
  
  show_focus : function (evt) // Show on focus
  {
    this.show_tip (evt, false);
  },
  
  show_click : function (evt)
  {
    this.show_tip (evt, false);
    if (this.target.nodeName.toLowerCase () == 'a') return EvtHandler.killEvt (evt); else return false;
  },
  
  toggle : function (evt)
  {
    if (this.popup.style.display != 'none' && this.popup.style.display != null) {
      this.hide_now (evt);
    } else {
      this.show_tip (evt, true);
    }
    if (this.target.nodeName.toLowerCase () == 'a') return EvtHandler.killEvt (evt); else return false;
  },

  show_tip : function (evt, is_mouse_evt)
  {
    if (this.hide_timeout_id != null) window.clearTimeout (this.hide_timeout_id);
    this.hide_timeout_id = null;
    if (this.popup.style.display != 'none' && this.popup.style.display != null) return;
    if (this.tip_creator) {
      // Dynamically created tooltip.
      try {
        this.content = this.tip_creator (evt);
      } catch (ex) {
        // Oops! Indicate that something went wrong!
        var error_msg = document.createElement ('div');
        error_msg.appendChild (
          document.createElement ('b').appendChild (
            document.createTextNode ('Exception: ' + ex.name)));
        error_msg.appendChild(document.createElement ('br'));
        error_msg.appendChild (document.createTextNode (ex.message));
        if (typeof (ex.fileName) != 'undefined' &&
            typeof (ex.lineNumber) != 'undefined') {
          error_msg.appendChild(document.createElement ('br'));
          error_msg.appendChild (document.createTextNode ('File ' + ex.fileName));
          error_msg.appendChild(document.createElement ('br'));
          error_msg.appendChild (document.createTextNode ('Line ' + ex.lineNumber));
        }
        this.content = error_msg;
      }
      // Our wrapper has at most two children: the close button, and the content. Don't remove
      // the close button, if any.
      if (this.popup.firstChild.lastChild && this.popup.firstChild.lastChild != this.close_button)
        this.popup.firstChild.removeChild (this.popup.firstChild.lastChild);
      this.popup.firstChild.appendChild (this.content);
      this.apply_styles (this.content, this.css);
      if (this.options.mode == Tooltip.TRACK) this.setHasLinks ();
    }
    // Position it now. It must be positioned before the timeout below!
    this.position_tip (evt, is_mouse_evt);
    if (Tooltips.debug) {
      alert ('Position: x = ' + this.popup.style.left + ' y = ' + this.popup.style.top);
    }
    this.setupEvents (EvtHandler.attach, 1);
    if (this.options.mode == Tooltip.TRACK) {
      if (this.has_links) {
        if (this.tracks) EvtHandler.remove (this.target, 'mousemove', this.eventTrack);
        this.tracks = false;
      } else {
        if (!this.tracks) EvtHandler.attach (this.target, 'mousemove', this.eventTrack);
        this.tracks = true;
      }
    }
    if (this.options.open_delay > 0) {
      var obj = this;
      this.open_timout_id = 
        window.setTimeout (function () {obj.show_now (obj);}, this.options.open_delay);
    } else
      this.show_now (this);
  },
  
  show_now : function (elem)
  {
    if (elem.popup.style.display != 'none' && elem.popup.style.display != null) return;
    Tooltips.register (elem);
    if (elem.ieFix) {
      elem.ieFix.style.top     = elem.popup.style.top;
      elem.ieFix.style.left    = elem.popup.style.left;
      elem.ieFix.style.width   = elem.size.width + "px";
      elem.ieFix.style.height  = elem.size.height + "px";
      elem.ieFix.style.display = "";
    }
    elem.popup.style.display = ""; // Finally show it
    if (   (elem.options.deactivate & Tooltip.ESCAPE)
        && typeof (elem.popup.focus) == 'function') {
      // We need to attach this event globally.
      EvtHandler.attach (document, 'keydown', elem.eventKey);
    }
    elem.open_timeout_id = null;
    // Callback
    if (typeof (elem.options.onopen) == 'function') elem.options.onopen (elem);
  },
  
  track : function (evt)
  {
    this.position_tip (evt, true);
    // Also move the shim!
    if (this.ieFix) {
      this.ieFix.style.top     = this.popup.style.top;
      this.ieFix.style.left    = this.popup.style.left;
      this.ieFix.style.width   = this.size.width + "px";
      this.ieFix.style.height  = this.size.height + "px";
    }
  },
  
  size_change : function ()
  {
    // If your content is such that it changes, make sure this is called after each size change.
    // Unfortunately, I have found no way of monitoring size changes of this.popup and then doing
    // this automatically. See for instance the "toggle" example (the 12th) on the example page at
    // http://commons.wikimedia.org/wiki/MediaWiki:Tooltips.js/Documentation/Examples
    if (this.popup.style.display != 'none' && this.popup.style.display != null) {
      // We're visible. Make sure the shim gets resized, too!
      this.size = {width : this.popup.offsetWidth, height: this.popup.offsetHeight};
      if (this.ieFix) {
        this.ieFix.style.top     = this.popup.style.top;
        this.ieFix.style.left    = this.popup.style.left;
        this.ieFix.style.width   = this.size.width + "px";
        this.ieFix.style.height  = this.size.height + "px";
      }
    }
  },
    
  position_tip : function (evt, is_mouse_evt)
  {
    var view = {width  : this.viewport ('Width'),
                height : this.viewport ('Height')};
    var off  = {left   : this.scroll_offset ('Left'),
                top    : this.scroll_offset ('Top')};
    var x = 0, y = 0;
    var offset = null;
    // Calculate the position
    if (is_mouse_evt && this.options.mode != Tooltip.FIXED) {
      var mouse_delta = EvtHandler.mouse_offset ();
      if (Tooltips.debug && mouse_delta) {
        alert ("Mouse offsets: x = " + mouse_delta.x + ", y = " + mouse_delta.y);
      }
      x = (evt.pageX || (evt.clientX + off.left - (mouse_delta ? mouse_delta.x : 0)));
      y = (evt.pageY || (evt.clientY + off.top - (mouse_delta ? mouse_delta.y : 0)));
      offset = 'mouse_offset';
    } else {
      var tgt = this.options.target || this.target;
      var pos = this.position (tgt);
      switch (this.options.anchor) {
        default:
        case Tooltip.BOTTOM_LEFT:
          x = pos.x; y = pos.y + tgt.offsetHeight;
          break;
        case Tooltip.BOTTOM_RIGHT:
          x = pos.x + tgt.offsetWidth; y = pos.y + tgt.offsetHeight;
          break;
        case Tooltip.TOP_LEFT:
          x = pos.x; y = pos.y;
          break;
        case Tooltip.TOP_RIGHT:
          x = pos.x + tgt.offsetWidth; y = pos.y;
          break;
      }
      offset = 'fixed_offset';
    }
    
    x = x + this.options[offset].x * this.options[offset].dx;
    y = y + this.options[offset].y * this.options[offset].dy;

    this.size = this.calculate_dimension ();
    if (this.options[offset].dx < 0) x = x - this.size.width;
    if (this.options[offset].dy < 0) y = y - this.size.height;
    
    // Now make sure we're within the view.
    if (x + this.size.width > off.left + view.width) x = off.left + view.width - this.size.width;
    if (x < off.left) x = off.left;
    if (y + this.size.height > off.top + view.height) y = off.top + view.height - this.size.height;
    if (y < off.top) y = off.top;
    
    this.popup.style.top  = y + "px";
    this.popup.style.left = x + "px";
  },
  
  hide : function (evt)
  {
    if (this.popup.style.display == 'none') return;
    // Get mouse position
    var mouse_delta = EvtHandler.mouse_offset ();
    var x = evt.pageX
         || (evt.clientX + this.scroll_offset ('Left') - (mouse_delta ? mouse_delta.x : 0));
    var y = evt.pageY
         || (evt.clientY + this.scroll_offset ('Top') - (mouse_delta ? mouse_delta.y : 0));
    // We hide it if we're neither within this.target nor within this.content nor within the
    // alternate target, if one was given.
    if (Tooltips.debug) {
      var tp = this.position (this.target);
      var pp = this.position (this.popup);
      alert ("x = " + x + " y = " + y + '\n' +
             "t: " + tp.x + "/" + tp.y + "/" +
               this.target.offsetWidth + "/" + this.target.offsetHeight + '\n' +
             (tp.n ? "t.m = " + tp.n.nodeName + "/" + tp.n.getAttribute ('margin') + "/"
                     + tp.n.getAttribute ('marginTop')
                     + "/" + tp.n.getAttribute ('border') + '\n'
                   : "") +
             "p: " + pp.x + "/" + pp.y + "/" +
               this.popup.offsetWidth + "/" + this.popup.offsetHeight + '\n' +
             (pp.n ? "p.m = " + pp.n.nodeName + "/" + pp.n.getAttribute ('margin') + "/"
                     + pp.n.getAttribute ('marginTop')
                     + "/" + pp.n.getAttribute ('border') + '\n'
                   : "") +
             "e: " + evt.pageX + "/" + evt.pageY + " "
               + evt.clientX + "/" + this.scroll_offset ('Left') + " "
               + evt.clientY + "/" + this.scroll_offset ('Top') + '\n' +
             (mouse_delta ? "m : " + mouse_delta.x + "/" + mouse_delta.y + '\n' : "")
             );
    }
    if (   !this.within (this.target, x, y)
        && !this.within (this.popup, x, y)
        && (!this.options.target || !this.within (this.options.target, x, y))) {
      if (this.open_timeout_id != null) window.clearTimeout (this.open_timeout_id);
      this.open_timeout_id = null;
      var event_copy = evt;
      if (this.options.hide_delay > 0) {
        var obj = this;
        this.hide_timeout_id =
          window.setTimeout (
              function () {obj.hide_popup (obj, event_copy);}
            , this.options.hide_delay
          );
      } else
        this.hide_popup (this, event_copy);
    }
  },
  
  hide_popup : function (elem, event)
  {
    if (elem.popup.style.display == 'none') return; // Already hidden, recursion from onclose?
    elem.popup.style.display = 'none';
    if (elem.ieFix) elem.ieFix.style.display = 'none';
    elem.hide_timeout_id = null;
    Tooltips.deregister (elem);
    if (elem.options.deactivate & Tooltip.ESCAPE)
      EvtHandler.remove (document, 'keydown', elem.eventKey);
    // Callback
    if (typeof (elem.options.onclose) == 'function') elem.options.onclose (elem, event);
  },
  
  hide_now : function (evt)
  {
    if (this.open_timeout_id != null) window.clearTimeout (this.open_timeout_id);
    this.open_timeout_id = null;
    var event_copy = evt || null;
    this.hide_popup (this, event_copy);
    if (evt && this.target.nodeName.toLowerCase == 'a') return EvtHandler.killEvt (evt); else return false;
  },
  
  key_handler : function (evt)
  {
    if (Tooltips.debug) alert ('key evt ' + evt.keyCode);
    if (evt.DOM_VK_ESCAPE && evt.keyCode == evt.DOM_VK_ESCAPE || evt.keyCode == 27)
      this.hide_now (evt);
    return true;
  },

  setZIndex : function (z_index)
  {
    if (z_index === null || isNaN (z_index) || z_index < 2) return;
    z_index = Math.floor (z_index);
    if (z_index == this.options.z_index) return; // No change
    if (this.ieFix) {
      // Always keep the shim below the actual popup.
      if (z_index > this.options.z_index) {
        this.popup.style.zIndex = z_index;
        this.ieFix.style.zIndex = "" + (z_index - 1);
      } else {
        this.ieFix.style.zIndex = "" + (z_index - 1);
        this.popup.style.zIndex = z_index;
      }
    } else {
      this.popup.style.zIndex = z_index;
    }
    this.options.z_index = z_index;
  },

  makeCloseButton : function ()
  {
    this.close_button = null;
    if (!this.options.close_button) return;
    var imgs = null;
    if (typeof (this.options.close_button.length) != 'undefined')
      imgs = this.options.close_button; // Also if it's a string (name of previously created class)
    else
      imgs = [this.options.close_button];
    if (!imgs || imgs.length == 0) return; // Paranoia
    var lk = Buttons.makeButton (imgs, this.tip_id + '_button', this.eventClose); 

    if (lk) {
      var width = lk.firstChild.getAttribute ('width');
      if (!is_IE) {
        lk.style.cssFloat = 'right';
      } else {
        // IE is incredibly broken on right floats.
        var container = document.createElement ('div');
        container.style.display      = 'inline';
        container.style.styleFloat   = 'right';
        container.appendChild (lk);
        lk = container;
      }
      lk.style.paddingTop   = '2px';
      lk.style.paddingRight = '2px';
      this.popup.firstChild.insertBefore (lk, this.popup.firstChild.firstChild);
      this.close_button = lk;
      this.close_button_width = parseInt ("" + width, 10);
    }
  },

  within : function (node, x, y)
  {
    if (!node) return false;
    var pos = this.position (node);
    return    (x == null || x > pos.x && x < pos.x + node.offsetWidth)
           && (y == null || y > pos.y && y < pos.y + node.offsetHeight);
  },
  
  position : (function ()
  {
    // The following is the jQuery.offset implementation. We cannot use jQuery yet in globally
    // activated scripts (it has strange side effects for Opera 8 users who can't log in anymore,
    // and it breaks the search box for some users). Note that jQuery does not support Opera 8.
    // Until the WMF servers serve jQuery by default, this copy from the jQuery 1.3.2 sources is
    // needed here. If and when we have jQuery available officially, the whole thing here can be
    // replaced by "var tmp = jQuery (node).offset(); return {x:tmp.left, y:tmp.top};"
    // Kudos to the jQuery development team. Any errors in this adaptation are my own. (Lupo,
    // 2009-08-24).
    //   Note: I have virtually the same code also in LAPI.js, but I cannot import that here
    // because I know that at least one gadget at the French Wikipedia includes this script here
    // directly from here. I'd have to use importScriptURI instead of importScript to keep that
    // working, but I'd run the risk that including LAPI at the French Wikipedia might break
    // something there. I *hate* it when people hotlink scripts across projects!

    var data = null;

    function jQuery_init ()
    {
      data = {};
      // Capability check from jQuery.
      var body = document.body;
      var container = document.createElement('div');
      var html =
          '<div style="position:absolute;top:0;left:0;margin:0;border:5px solid #000;'
        + 'padding:0;width:1px;height:1px;"><div></div></div><table style="position:absolute;'
        + 'top:0;left:0;margin:0;border:5px solid #000;padding:0;width:1px;height:1px;" '
        + 'cellpadding="0" cellspacing="0"><tr><td></td></tr></table>';
      var rules = { position: 'absolute', visibility: 'hidden'
                   ,top: 0, left: 0
                   ,margin: 0, border: 0
                   ,width: '1px', height: '1px'
                  };
      Object.merge (rules, container.style);

      container.innerHTML = html;
      body.insertBefore(container, body.firstChild);
      var innerDiv = container.firstChild;
      var checkDiv = innerDiv.firstChild;
      var td = innerDiv.nextSibling.firstChild.firstChild;

      data.doesNotAddBorder = (checkDiv.offsetTop !== 5);
      data.doesAddBorderForTableAndCells = (td.offsetTop === 5);

      innerDiv.style.overflow = 'hidden', innerDiv.style.position = 'relative';
      data.subtractsBorderForOverflowNotVisible = (checkDiv.offsetTop === -5);

      var bodyMarginTop    = body.style.marginTop;
      body.style.marginTop = '1px';
      data.doesNotIncludeMarginInBodyOffset = (body.offsetTop === 0);
      body.style.marginTop = bodyMarginTop;

      body.removeChild(container);
    };

    function jQuery_offset (node)
    {
      if (node === node.ownerDocument.body) return jQuery_bodyOffset (node);
      if (node.getBoundingClientRect) {
        var box    = node.getBoundingClientRect ();
        var scroll = {x : this.scroll_offset ('Left'), y: this.scroll_offset ('Top')};
        return {x : (box.left + scroll.x), y : (box.top + scroll.y)};
      }
      if (!data) jQuery_init ();
      var elem              = node;
      var offsetParent      = elem.offsetParent;
      var prevOffsetParent  = elem;
      var doc               = elem.ownerDocument;
      var prevComputedStyle = doc.defaultView.getComputedStyle(elem, null);
      var computedStyle;

      var top  = elem.offsetTop;
      var left = elem.offsetLeft;

      while ( (elem = elem.parentNode) && elem !== doc.body && elem !== doc.documentElement ) {
        computedStyle = doc.defaultView.getComputedStyle(elem, null);
        top -= elem.scrollTop, left -= elem.scrollLeft;
        if ( elem === offsetParent ) {
          top += elem.offsetTop, left += elem.offsetLeft;
          if (   data.doesNotAddBorder
              && !(data.doesAddBorderForTableAndCells && /^t(able|d|h)$/i.test(elem.tagName))
             )
          {
            top  += parseInt (computedStyle.borderTopWidth,  10) || 0;
            left += parseInt (computedStyle.borderLeftWidth, 10) || 0;
          }
          prevOffsetParent = offsetParent; offsetParent = elem.offsetParent;
        }
        if (data.subtractsBorderForOverflowNotVisible && computedStyle.overflow !== 'visible')
        {
          top  += parseInt (computedStyle.borderTopWidth,  10) || 0;
          left += parseInt (computedStyle.borderLeftWidth, 10) || 0;
        }
        prevComputedStyle = computedStyle;
      }

      if (prevComputedStyle.position === 'relative' || prevComputedStyle.position === 'static') {
        top  += doc.body.offsetTop;
        left += doc.body.offsetLeft;
      }
      if (prevComputedStyle.position === 'fixed') {
        top  += Math.max (doc.documentElement.scrollTop, doc.body.scrollTop);
        left += Math.max (doc.documentElement.scrollLeft, doc.body.scrollLeft);
      }
      return {x: left, y: top};            
    }

    function jQuery_bodyOffset (body)
    {
      if (!data) jQuery_init();
      var top = body.offsetTop, left = body.offsetLeft;
      if (data.doesNotIncludeMarginInBodyOffset) {
        var styles;
        if (   body.ownerDocument.defaultView
            && body.ownerDocument.defaultView.getComputedStyle)
        { // Gecko etc.
          styles = body.ownerDocument.defaultView.getComputedStyle (body, null);
          top  += parseInt (style.getPropertyValue ('margin-top' ), 10) || 0;
          left += parseInt (style.getPropertyValue ('margin-left'), 10) || 0;
        } else {
          function to_px (element, val) {
            // Convert em etc. to pixels. Kudos to Dean Edwards; see
            // http://erik.eae.net/archives/2007/07/27/18.54.15/#comment-102291
            if (!/^\d+(px)?$/i.test (val) && /^\d/.test (val) && body.runtimeStyle) {
              var style                 = element.style.left;
              var runtimeStyle          = element.runtimeStyle.left;
              element.runtimeStyle.left = element.currentStyle.left;
              element.style.left        = result || 0;
              val = elem.style.pixelLeft + "px";
              element.style.left        = style;
              element.runtimeStyle.left = runtimeStyle;
            }
            return val;
          }
          style = body.currentStyle || body.style;
          top  += parseInt (to_px (body, style.marginTop ), 10) || 0;
          left += parseInt (to_px (body, style.marginleft), 10) || 0;
        }
      }
      return {x: left, y: top};
    }

    return jQuery_offset;
  })(),

  scroll_offset : function (what)
  {
    var s = 'scroll' + what;
    return (document.documentElement ? document.documentElement[s] : 0)
           || document.body[s] || 0;
  },

  viewport : function (what)
  {
    if (   typeof (is_opera_95) != 'undefined' && is_opera_95 && what == 'Height'
        || typeof (is_safari) != 'undefined' && is_safari && !document.evaluate)
      return window['inner' + what];
    var s = 'client' + what;
    if (typeof (is_opera) != 'undefined' && is_opera) return document.body[s];
    return (document.documentElement ? document.documentElement[s] : 0)
           || document.body[s] || 0;
  },


  calculate_dimension : function ()
  {
    if (this.popup.style.display != 'none' && this.popup.style.display != null) {
      return {width : this.popup.offsetWidth, height : this.popup.offsetHeight};
    }
    // Must display it... but position = 'absolute' and visibility = 'hidden' means
    // the user won't notice it.
    var view_width = this.viewport ('Width');
    this.popup.style.top        = "0px";
    this.popup.style.left       = "0px";
    // Remove previous width as it may change with dynamic tooltips
    this.popup.style.width      = "";
    this.popup.style.maxWidth   = "";
    this.popup.style.overflow   = 'hidden';
    if (typeof (is_opera) != 'undefined' && is_opera) {
      // Just put it out of the way. Opera has an ugly bug: textareas that were once hidden
      // through visibility settings don't work anymore when they're displayed. They don't
      // fire events, may or may not update their text display when modified through Javascript,
      // and finally they may even refuse to get or have their value set through JS at all.
      // If our tooltip content contains a textarea, we'd run into this longstanding bug if
      // we used visibility on Opera. Hence we don't.
      // For a report of this bug, see http://www.quirksmode.org/bugreports/archives/2005/06/Cant_change_the_value_of_textarea_after_hiding_it_.html
      // from June 30, 2005 (that would've been about Opera 7.5). I can confirm that this bug
      // is still in Opera 9.63, though I have not been able to reproduce with a reduced example.
      // It appears that in addition to using visibility, there is some other ingredient here
      // that triggers this bug. In any case, not using visibility avoids the bug.
      // Lupo, 2009-06-10
      this.popup.style.top = '-10000px';
      this.popup.style.left = '-10000px';
    } else
      this.popup.style.visibility = 'hidden';
    // Remove the close button, otherwise the float will always extend the box to
    // the right edge.
    if (this.close_button)
      this.popup.firstChild.removeChild (this.close_button);
    this.popup.style.display = "";   // Display it. Now we should have a width
    var w = this.popup.offsetWidth;
    var h = this.popup.offsetHeight;
    var limit = Math.round (view_width * this.options.max_width);
    if (this.options.max_pixels > 0 && this.options.max_pixels < limit)
      limit = this.options.max_pixels;
    if (w > limit) {
      w = limit;
      this.popup.style.width    = "" + w + "px";
      this.popup.style.maxWidth = this.popup.style.width;
      if (this.close_button) {
        this.popup.firstChild.insertBefore
          (this.close_button, this.popup.firstChild.firstChild);
      }
    } else {
      this.popup.style.width    = "" + w + "px";
      this.popup.style.maxWidth = this.popup.style.width;
      if (this.close_button) {
        this.popup.firstChild.insertBefore
          (this.close_button, this.popup.firstChild.firstChild);
      }
      if (h != this.popup.offsetHeight) {
        w =  w + this.close_button_width;    
        this.popup.style.width    = "" + w + "px";
        this.popup.style.maxWidth = this.popup.style.width;
      }
    }
    var size = {width : this.popup.offsetWidth, height : this.popup.offsetHeight};
    this.popup.style.display = 'none';       // Hide it again
    if (typeof (is_opera) != 'undefined' && is_opera) {
      this.popup.style.top = '0px'; // Clean up, even if not really necessary
      this.popup.style.left = '0px';
    } else
      this.popup.style.visibility = "";
    return size;
  }
    
} // end Tooltip

// </source>