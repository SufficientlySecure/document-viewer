// <source lang=javascript">

/*
  Small JS library containing stuff I use often.
  
  Author: [[User:Lupo]], June 2009
  License: Quadruple licensed GFDL, GPL, LGPL and Creative Commons Attribution 3.0 (CC-BY-3.0)
 
  Choose whichever license of these you like best :-)

  Includes the following components:
   - Object enhancements (clone, merge)
   - String enhancements (trim, ...)
   - Array enhancements (JS 1.6)
   - Function enhancements (bind)
   - LAPI            Most basic DOM functions: $ (getElementById), make
   -   LAPI.Ajax     Ajax request implementation, tailored for MediaWiki/WMF sites
   -   LAPI.Browser  Browser detection (general)
   -   LAPI.DOM      DOM helpers, including a cross-browser DOM parser
   -   LAPI.WP       MediaWiki/WMF-specific DOM routines
   -   LAPI.Edit     Simple editor implementation with save, cancel, preview (for WMF sites)
   -   LAPI.Evt      Event handler routines (general)
   -   LAPI.Pos      Position calculations (general)
*/

// Global: wgServer, wgScript, wgUserLanguage, injectSpinner, removeSpinner (from wiki.js)
// Global: importScript (from wiki.js, for MediaWiki:AjaxSubmit.js)

// Configuration: set this to the URL of your image server. The value is a string representation
// of a regular expression. For instance, for Wikia, use "http://images\\d\\.wikia\\.nocookie\\.net".
// Remember to double-escape the backslash.
if (typeof (LAPI_file_store) == 'undefined')
  var LAPI_file_store = "(https?:)?//upload\\.wikimedia\\.org/";

// Some basic routines, mainly enhancements of the String, Array, and Function objects.
// Some taken from Javascript 1.6, some own.

/** Object enhancements ************/

// Note: adding these to the prototype may break other code that assumes that
// {} has no properties at all.
Object.clone = function (source, includeInherited)
{
  if (!source) return null;
  var result = {};
  for (var key in source) {
    if (includeInherited || source.hasOwnProperty (key)) result[key] = source[key];
  }
  return result;
};

Object.merge = function (from, into, includeInherited)
{
  if (!from) return into;
  for (var key in from) {
    if (includeInherited || from.hasOwnProperty (key)) into[key] = from[key];
  }
  return into;
};

Object.mergeSome = function (from, into, includeInherited, predicate)
{
  if (!from) return into;
  if (typeof (predicate) == 'undefined')
    return Object.merge (from, into, includeInherited);
  for (var key in from) {
    if ((includeInherited || from.hasOwnProperty (key)) && predicate (from, into, key))
      into[key] = from[key];
  }
  return into;
};

Object.mergeSet = function (from, into, includeInherited)
{
  return Object.mergeSome
           (from, into, includeInherited, function (src, tgt, key) {return src[key] != null;});
}

/** String enhancements (Javascript 1.6) ************/

// Removes given characters from both ends of the string.
// If no characters are given, defaults to removing whitespace.
if (!String.prototype.trim) {
  String.prototype.trim = function (chars) {
    if (!chars) return this.replace (/^\s+|\s+$/g, "");
    return this.trimRight (chars).trimLeft (chars);
  };
}

// Removes given characters from the beginning of the string.
// If no characters are given, defaults to removing whitespace.
if (!String.prototype.trimLeft) {
  String.prototype.trimLeft = function (chars) {
    if (!chars) return this.replace (/^\s\s*/, "");
    return this.replace (new RegExp ('^[' + chars.escapeRE () + ']+'), "");
  };
}
String.prototype.trimFront = String.prototype.trimLeft; // Synonym

// Removes given characters from the end of the string.
// If no characters are given, defaults to removing whitespace.
if (!String.prototype.trimRight) {
  String.prototype.trimRight = function (chars) {
    if (!chars) return this.replace (/\s\s*$/, "");
    return this.replace (new RegExp ('[' + chars.escapeRE () + ']+$'), "");
  };
}
String.prototype.trimEnd = String.prototype.trimRight; // Synonym

/** Further String enhancements ************/

// Returns true if the string begins with prefix.
String.prototype.startsWith = function (prefix) {
  return this.indexOf (prefix) == 0;
};

// Returns true if the string ends in suffix
String.prototype.endsWith = function (suffix) {
  return this.lastIndexOf (suffix) + suffix.length == this.length;
};

// Returns true if the string contains s.
String.prototype.contains = function (s) {
  return this.indexOf (s) >= 0;
};

// Replace all occurrences of a string pattern by replacement.
String.prototype.replaceAll = function (pattern, replacement) {
  return this.split (pattern).join (replacement);
};

// Escape all backslashes and single or double quotes such that the result can
// be used in Javascript inside quotes or double quotes.
String.prototype.stringifyJS = function () {
  return this.replace (/([\\\'\"]|%5C|%27|%22)/g, '\\$1') // ' // Fix syntax coloring
             .replace (/\n/g, '\\n');
}

// Escape all RegExp special characters such that the result can be safely used
// in a RegExp as a literal.
String.prototype.escapeRE = function () {
  return this.replace (/([\\{}()|.?*+^$\[\]])/g, "\\$1");
};

String.prototype.escapeXML = function (quot, apos) {
  var s    = this.replace (/&/g,    '&amp;')
                 .replace (/\xa0/g, '&nbsp;')
                 .replace (/</g,    '&lt;')
                 .replace (/>/g,    '&gt;');
  if (quot) s = s.replace (/\"/g,   '&quot;'); // " // Fix syntax coloring
  if (apos) s = s.replace (/\'/g,   '&apos;'); // ' // Fix syntax coloring
  return s;
};

String.prototype.decodeXML = function () {
  return this.replace(/&quot;/g, '"')
             .replace(/&apos;/g, "'")
             .replace(/&gt;/g,   '>')
             .replace(/&lt;/g,   '<')
             .replace(/&nbsp;/g, '\xa0')
             .replace(/&amp;/g,  '&');
};

String.prototype.capitalizeFirst = function () {
  return this.substring (0, 1).toUpperCase() + this.substring (1);
};

String.prototype.lowercaseFirst = function () {
  return this.substring (0, 1).toLowerCase() + this.substring (1);
};

// This is actually a function on URLs, but since URLs typically are strings in
// Javascript, let's include this one here, too.
String.prototype.getParamValue = function (param) {
  var re = new RegExp ('[&?]' + param.escapeRE () + '=([^&#]*)');
  var m  = re.exec (this);
  if (m && m.length >= 2) return decodeURIComponent (m[1]);
  return null;
};

String.getParamValue = function (param, url)
{
  if (typeof (url) == 'undefined' || url === null) url = document.location.href;
  try {
    return url.getParamValue (param);
  } catch (e) { 
    return null;
  }
};

/** Function enhancements ************/

// Return a function that calls the function with 'this' bound to 'thisObject'
if (!Function.prototype.bind) { // JS 1.8.5 has bind()
  Function.prototype.bind = function (thisObject) {
    var f = this, obj = thisObject;
    return function () { return f.apply (obj, arguments); };
  };
}

/** Array enhancements (Javascript 1.6) ************/

// Note that contrary to JS 1.6, we treat the thisObject as optional.
// Don't add to the prototype, that would break for (var key in array) loops!

// Returns a new array containing only those elements for which predicate
// is true.
if (!Array.filter) {
  Array.filter = function (target, predicate, thisObject)
  {
    if (target === null) return null;
    if (typeof (target.filter) == 'function') return target.filter (predicate, thisObject);
    if (typeof (predicate) != 'function')
      throw new Error ('Array.filter: predicate must be a function');
    var l = target.length;
    var result = [];
    if (thisObject) predicate = predicate.bind (thisObject);
    for (var i=0; l && i < l; i++) {
      if (i in target) {
        var curr = target[i];
        if (predicate (curr, i, target)) result[result.length] = curr;
      }
    }
    return result;
  };
}
Array.select = Array.filter; // Synonym

// Calls iterator on all elements of the array
if (!Array.forEach) {
  Array.forEach = function (target, iterator, thisObject)
  {
    if (target === null) return;
    if (typeof (target.forEach) == 'function') {
      target.forEach (iterator, thisObject);
      return;
    }
    if (typeof (iterator) != 'function')
      throw new Error ('Array.forEach: iterator must be a function');
    var l = target.length;
    if (thisObject) iterator = iterator.bind (thisObject);
    for (var i=0; l && i < l; i++) {
      if (i in target) iterator (target[i], i, target);
    }
  };
}

// Returns true if predicate is true for every element of the array, false otherwise
if (!Array.every) {
  Array.every = function (target, predicate, thisObject)
  {
    if (target === null) return true;
    if (typeof (target.every) == 'function') return target.every (predicate, thisObject);
    if (typeof (predicate) != 'function')
      throw new Error ('Array.every: predicate must be a function');
    var l = target.length;
    if (thisObject) predicate = predicate.bind (thisObject);
    for (var i=0; l && i < l; i++) {
      if (i in target && !predicate (target[i], i, target)) return false;
    }
    return true;
  };
}
Array.forAll = Array.every; // Synonym

// Returns true if predicate is true for at least one element of the array, false otherwise.
if (!Array.some) {
  Array.some = function (target, predicate, thisObject)
  {
    if (target === null) return false;
    if (typeof (target.some) == 'function') return target.some (predicate, thisObject);
    if (typeof (predicate) != 'function')
      throw new Error ('Array.some: predicate must be a function');
    var l = target.length;
    if (thisObject) predicate = predicate.bind (thisObject);
    for (var i=0; l && i < l; i++) {
      if (i in target && predicate (target[i], i, target)) return true;
    }
    return false;
  };
}
Array.exists = Array.some; // Synonym

// Returns a new array built by applying mapper to all elements.
if (!Array.map) {
  Array.map = function (target, mapper, thisObject)
  {
    if (target === null) return null;
    if (typeof (target.map) == 'function') return target.map (mapper, thisObject);
    if (typeof (mapper) != 'function')
      throw new Error ('Array.map: mapper must be a function');
    var l = target.length;
    var result = [];
    if (thisObject) mapper = mapper.bind (thisObject);
    for (var i=0; l && i < l; i++) {
      if (i in target) result[i] = mapper (target[i], i, target);
    }
    return result;
  };
}

if (!Array.indexOf) {
  Array.indexOf = function (target, elem, from)
  {
    if (target === null) return -1;
    if (typeof (target.indexOf) == 'function') return target.indexOf (elem, from);
    if (typeof (target.length) == 'undefined') return -1;
    var l = target.length;    
    if (isNaN (from)) from = 0; else from = from || 0;
    from = (from < 0) ? Math.ceil (from) : Math.floor (from);
    if (from < 0) from += l;
    if (from < 0) from = 0;
    while (from < l) {
      if (from in target && target[from] === elem) return from;
      from += 1;
    }
    return -1;
  };
}

if (!Array.lastIndexOf) {
  Array.lastIndexOf = function (target, elem, from)
  {
    if (target === null) return -1;
    if (typeof (target.lastIndexOf) == 'function') return target.lastIndexOf (elem, from);
    if (typeof (target.length) == 'undefined') return -1;
    var l = target.length;
    if (isNaN (from)) from = l-1; else from = from || (l-1);
    from = (from < 0) ? Math.ceil (from) : Math.floor (from);
    if (from < 0) from += l; else if (from >= l) from = l-1;
    while (from >= 0) {
      if (from in target && target[from] === elem) return from;
      from -= 1;
    }
    return -1;
  };
}

/** Additional Array enhancements ************/

Array.remove = function (target, elem) {
  var i = Array.indexOf (target, elem);
  if (i >= 0) target.splice (i, 1);
};

Array.contains = function (target, elem) {
  return Array.indexOf (target, elem) >= 0;
};

Array.flatten = function (target) {
  var result = [];
  Array.forEach (target, function (elem) {result = result.concat (elem);});
  return result;
};

// Calls selector on the array elements until it returns a non-null object
// and then returns that object. If selector always returns null, any also
// returns null. See also Array.map.
Array.any = function (target, selector, thisObject)
{
  if (target === null) return null;
  if (typeof (selector) != 'function')
    throw new Error ('Array.any: selector must be a function');
  var l = target.length;
  var result = null;
  if (thisObject) selector = selector.bind (thisObject);
  for (var i=0; l && i < l; i++) {
    if (i in target) {
      result = selector (target[i], i, target);
      if (result != null) return result;
    }
  }
  return null;
};

// Return a contiguous array of the contents of source, which may be an array or pseudo-array,
// basically anything that has a length and can be indexed. (E.g. live HTMLCollections, but also
// Strings, or objects, or the arguments "variable".
Array.make = function (source)
{
  if (!source || typeof (source.length) == 'undefined') return null;
  var result = [];
  var l      = source.length;
  for (var i=0; i < l; i++) {
    if (i in source) result[result.length] = source[i];
  }
  return result;
};

if (typeof (window.LAPI) == 'undefined') {

window.LAPI = {
  Ajax :
  {
    getRequest : function ()
    {
      var request = null;
      try {
        request = new XMLHttpRequest();
      } catch (anything) {
        request = null;
        if (!!window.ActiveXObject) {
          if (typeof (LAPI.Ajax.getRequest.msXMLHttpID) == 'undefined') {
            var XHR_ids = [  'MSXML2.XMLHTTP.6.0', 'MSXML2.XMLHTTP.3.0'
                           , 'MSXML2.XMLHTTP', 'Microsoft.XMLHTTP'
                          ];
            for (var i=0; i < XHR_ids.length && !request; i++) {
              try {
                request = new ActiveXObject (XHR_ids[i]);
                if (request) LAPI.Ajax.getRequest.msXMLHttpID = XHR_ids[i];
              } catch (ex) {
                request = null;
              }
            }
            if (!request) LAPI.Ajax.getRequest.msXMLHttpID = null;
          } else if (LAPI.Ajax.getRequest.msXMLHttpID) {
            request = new ActiveXObject (LAPI.Ajax.getRequest.msXMLHttpID);
          }
        } // end if IE
      } // end try-catch
      return request;
    }
  },

  $ : function (selector, doc, multi)
  {
    if (!selector || selector.length == 0) return null;
    doc = doc || document;
    if (typeof (selector) == 'string') {
      if (selector.charAt (0) == '#') selector = selector.substring (1);
      if (selector.length > 0) return doc.getElementById (selector);
      return null;
    } else {
      if (multi) return Array.map (selector, function (id) {return LAPI.$ (id, doc);});
      return Array.any (selector, function (id) {return LAPI.$ (id, doc);});
    }
  },

  make : function (tag, attribs, css, doc)
  {
    doc = doc || document;
    if (!tag || tag.length == 0) throw new Error ('No tag for LAPI.make');
    var result = doc.createElement (tag);
    Object.mergeSet (attribs, result);
    Object.mergeSet (css, result.style);
    if (/^(form|input|button|select|textarea)$/.test (tag) &&
        result.id && result.id.length > 0 && !result.name
       )
    {
      result.name = result.id;
    }
    return result;
  },

  formatException : function (ex, asDOM)
  {
    var name = ex.name || "";
    var msg  = ex.message || "";
    var file = null;
    var line = null;
    if (msg && msg.length > 0 && msg.charAt (0) == '#') {
      // User msg: don't confuse users with error locations. (Note: could also use
      // custom exception types, but that doesn't work right on IE6.)
      msg = msg.substring (1);
    } else {
      file = ex.fileName || ex.sourceURL || null; // Gecko, Webkit, others
      line = ex.lineNumber || ex.line || null;    // Gecko, Webkit, others
    }
    if (name || msg) {
      if (!asDOM) {
        return
          'Exception ' + name + ': ' + msg
          + (file ? '\nFile ' + file + (line ? ' (' + line + ')' : "") : "")
          ;
      } else {
        var ex_msg = LAPI.make ('div');
        ex_msg.appendChild (document.createTextNode ('Exception ' + name + ': ' + msg));
        if (file) {
          ex_msg.appendChild (LAPI.make ('br'));
          ex_msg.appendChild
            (document.createTextNode ('File ' + file + (line ? ' (' + line + ')' : "")));
        }
        return ex_msg;
      }
    } else {
      return null;
    }
  }

};

} // end if (guard)

if (typeof (LAPI.Browser) == 'undefined') {
  
// Yes, usually it's better to test for available features. But sometimes there's no
// way around testing for specific browsers (differences in dimensions, layout errors,
// etc.)
LAPI.Browser =
(function (agent) {
  var result = {};
  result.client = agent;
  var m = agent.match(/applewebkit\/(\d+)/);
  result.is_webkit = (m != null);
  result.is_safari = result.is_webkit && !agent.contains ('spoofer');
  result.webkit_version = (m ? parseInt (m[1]) : 0);
  result.is_khtml =
       navigator.vendor == 'KDE'
    || (document.childNodes && !document.all && !navigator.taintEnabled && navigator.accentColorName);
  result.is_gecko =
       agent.contains ('gecko')
    && !/khtml|spoofer|netscape\/7\.0/.test (agent);
  result.is_ff_1    = agent.contains ('firefox/1');
  result.is_ff_2    = agent.contains ('firefox/2');
  result.is_ff_ge_2 = /firefox\/[2-9]|minefield\/3/.test (agent);
  result.is_ie      = agent.contains ('msie') || !!window.ActiveXObject;
  result.is_ie_lt_7 = false;
  if (result.is_ie) {
    var version = /msie ((\d|\.)+)/.exec (agent);
    result.is_ie_lt_7  = (version != null && (parseFloat(version[1]) < 7));
  }
  result.is_opera      = agent.contains ('opera');
  result.is_opera_ge_9 = false;
  result.is_opera_95   = false;
  if (result.is_opera) {
    m = /opera\/((\d|\.)+)/.exec (agent);
    result.is_opera_95   = m && (parseFloat (m[1]) >= 9.5);
    result.is_opera_ge_9 = m && (parseFloat (m[1]) >= 9.0);
  }
  result.is_mac = agent.contains ('mac');
  return result;
})(navigator.userAgent.toLowerCase ());

} // end if (guard)

if (typeof (LAPI.DOM) == 'undefined') {
  
LAPI.DOM =
{
  // IE6 doesn't have these Node constants in Node, so put them here
  ELEMENT_NODE                :  1,
  ATTRIBUTE_NODE              :  2,
  TEXT_NODE                   :  3,
  CDATA_SECTION_NODE          :  4,
  ENTITY_REFERENCE_NODE       :  5,
  ENTITY_NODE                 :  6,
  PROCESSING_INSTRUCTION_NODE :  7,
  COMMENT_NODE                :  8,
  DOCUMENT_NODE               :  9,
  DOCUMENT_TYPE_NODE          : 10,
  DOCUMENT_FRAGMENT_NODE      : 11,
  NOTATION_NODE               : 12,

  cleanAttributeName : function (attr_name)
  {
    if (!LAPI.Browser.is_ie) return attr_name;
    if (!LAPI.DOM.cleanAttributeName._names) {
      LAPI.DOM.cleanAttributeName._names = {
         'class'       : 'className'
        ,'cellspacing' : 'cellSpacing'
        ,'cellpadding' : 'cellPadding'
        ,'colspan'     : 'colSpan'
        ,'maxlength'   : 'maxLength'
        ,'readonly'    : 'readOnly'
        ,'rowspan'     : 'rowSpan'
        ,'tabindex'    : 'tabIndex'
        ,'valign'      : 'vAlign'
      };
    }
    var cleaned = attr_name.toLowerCase ();
    return LAPI.DOM.cleanAttributeName._names[cleaned] || cleaned;
  },

  importNode : function (into, node, deep)
  {
    if (!node) return null;
    if (into.importNode) return into.importNode (node, deep);
    if (node.ownerDocument == into) return node.cloneNode (deep);
    var new_node = null;
    switch (node.nodeType) {
      case LAPI.DOM.ELEMENT_NODE :
        new_node = into.createElement (node.nodeName);
        Array.forEach (
            node.attributes
          , function (attr) {
              if (attr && attr.nodeValue && attr.nodeValue.length > 0)
                new_node.setAttribute (LAPI.DOM.cleanAttributeName (attr.name), attr.nodeValue);
            }
        );
        new_node.style.cssText = node.style.cssText;
        if (deep) {
          Array.forEach (
              node.childNodes
            , function (child) {
                var copy = LAPI.DOM.importNode (into, child, true);
                if (copy) new_node.appendChild (copy);
              }
          );
        }
        return new_node;
      case LAPI.DOM.TEXT_NODE :
        return into.createTextNode (node.nodeValue);
      case LAPI.DOM.CDATA_SECTION_NODE :
        return (into.createCDATASection
                  ? into.createCDATASection (node.nodeValue)
                  : into.createTextNode (node.nodeValue)
               );
      case LAPI.DOM.COMMENT_NODE :
        return into.createComment (node.nodeValue);
      default :
        return null;
    } // end switch
  },

  parse : function (str, content_type)
  {
    function getDocument (str, content_type)
    {
      if (typeof (DOMParser) != 'undefined') {
        var parser = new DOMParser ();
        if (parser && parser.parseFromString)
          return parser.parseFromString (str, content_type);
      }
      // We don't have DOMParser
      if (LAPI.Browser.is_ie) {
        var doc = null;
        // Apparently, these can be installed side-by-side. Try to get the newest one available.
        // Unfortunately, one finds a variety of version strings on the net. I have no idea which
        // ones are correct.
        if (typeof (LAPI.DOM.parse.msDOMDocumentID) == 'undefined') {
          // If we find a parser, we cache it. If we cannot find one, we also remember that.
          var parsers =
            [ 'MSXML6.DOMDocument','MSXML5.DOMDocument','MSXML4.DOMDocument','MSXML3.DOMDocument'
             ,'MSXML2.DOMDocument.5.0','MSXML2.DOMDocument.4.0','MSXML2.DOMDocument.3.0'
             ,'MSXML2.DOMDocument','MSXML.DomDocument','Microsoft.XmlDom'];
          for (var i=0; i < parsers.length && !doc; i++) {
            try {
              doc = new ActiveXObject (parsers[i]);
              if (doc) LAPI.DOM.parse.msDOMDocumentID = parsers[i];
            } catch (ex) {
              doc = null;
            }
          }
          if (!doc) LAPI.DOM.parse.msDOMDocumentID = null;
        } else if (LAPI.DOM.parse.msDOMDocumentID) {
          doc = new ActiveXObject (LAPI.DOM.parse.msDOMDocumentID);
        }
        if (doc) {
          doc.async = false;
          doc.loadXML (str);
          return doc;
        }
      } 
      // Try using a "data" URI (http://www.ietf.org/rfc/rfc2397). Reported to work on
      // older Safaris.
      content_type  = content_type || 'application/xml';
      var req = LAPI.Ajax.getRequest ();
      if (req) {
        // Synchronous is OK, since "data" URIs are local
        req.open
          ('GET', 'data:' + content_type + ';charset=utf-8,' + encodeURIComponent (str), false);
        if (req.overrideMimeType) req.overrideMimeType (content_type);
        req.send (null);
        return req.responseXML;
      }
      return null;
    } // end getDocument

    var doc = null;

    try {
      doc = getDocument (str, content_type);
    } catch (ex) {
      doc = null;
    }
    if (   (    (!doc || !doc.documentElement)
             && (   str.search (/^\s*(<xml[^>]*>\s*)?<!doctype\s+html/i) >= 0
                 || str.search (/^\s*<html/i) >= 0
                )
           )
        ||
           (doc && (   LAPI.Browser.is_ie
                    && (!doc.documentElement
                        && doc.parseError && doc.parseError.errorCode != 0
                        && doc.parseError.reason.contains ('Error processing resource')
                        && doc.parseError.reason.contains
                             ('http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd')
                       )
                   )
           )
       )
    {
      // Either the text specified an (X)HTML document, but we failed to get a Document, or we
      // hit the walls of the single-origin policy on IE which tries to get the DTD from the
      // URI specified... Let's fake a document:
      doc = LAPI.DOM.fakeHTMLDocument (str);
    }
    return doc;
  },

  parseHTML : function (str, sanity_check)
  {
    // Simplified from the above, for cases where we *know* up front that the text is (X)HTML.
    var doc = null;
    if (typeof (DOMParser) != 'undefined') {
      var parser = new DOMParser ();
      if (parser && parser.parseFromString)
        doc = parser.parseFromString (str, 'text/xml');
    }
    if (!doc || !doc.documentElement || /^parsererror$/i.test (doc.documentElement.tagName)
        || (sanity_check && doc.getElementById (sanity_check) == null))
    {
      // We had an error, or the sanity check (looking for an element known to be there) failed.
      // (Happens on Konqueror 4.2.3/4.2.4 upon the very first call...)
      doc = LAPI.DOM.fakeHTMLDocument (str);
    }
    return doc;
  },

  fakeHTMLDocument : function (str)
  {
    var body_tag = /<body.*?>/.exec (str);
    if (!body_tag || body_tag.length == 0) return null;
    body_tag = body_tag.index + body_tag[0].length; // Index after the opening body tag
    var body_end = str.lastIndexOf ('</body>');
    if (body_end < 0) return null;
    var content = str.substring (body_tag, body_end); // Anything in between          
    content = content.replace(/<script(.|\s)*?\/script>/g, ""); // Sanitize: strip scripts
    return new LAPI.DOM.DocumentFacade (content);
  },

  isValid : function (doc)
  {
    if (!doc) return doc;
    if (typeof (doc.parseError) != 'undefined') { // IE
      if (doc.parseError.errorCode != 0) {
        throw new Error (  'XML parse error: ' + doc.parseError.reason
                         + ' line ' + doc.parseError.line
                         + ' col ' + doc.parseError.linepos
                         + '\nsrc = ' + doc.parseError.srcText);
      }
    } else {
      // FF... others?
      var root = doc.documentElement;
      if (/^parsererror$/i.test (root.tagName)) {
        throw new Error ('XML parse error: ' + root.getInnerText ());
      }
    }
    return doc;
  },

  hasClass : function (node, className)
  {
    if (!node) return false;
    return (' ' + node.className + ' ').contains (' ' + className + ' ');
  },
  
  setContent : function (node, content)
  {
    if (content == null) return node;
    LAPI.DOM.removeChildren (node);
    if (content.nodeName) { // presumably a DOM tree, like a span or a document fragment
      node.appendChild (content);
    } else if (typeof (node.innerHTML) != 'undefined') {
      node.innerHTML = content.toString ();
    } else {
      node.appendChild (document.createTextNode (content.toString ()));
    }
    return node;
  },

  makeImage : function (src, width, height, title, doc)
  {
    return LAPI.make (
               'img'
             , {src : src, width: "" + width, height : "" + height, title : title}
             , doc
           );
  },

  makeButton : function (id, text, f, submit, doc)
  {
    return LAPI.make (
               'input'
             , {id : id || "", type: (submit ? 'submit' : 'button'), value: text, onclick: f}
             , doc
           );
  },
  
  makeLabel : function (id, text, for_elem, doc)
  {
    var label = LAPI.make ('label', {id: id || "", htmlFor: for_elem}, null, doc);
    return LAPI.DOM.setContent (label, text);
  },
  
  makeLink : function (url, text, tooltip, onclick, doc)
  {
    var lk = LAPI.make ('a', {href: url, title: tooltip, onclick: onclick}, null, doc);
    return LAPI.DOM.setContent (lk, text || url);
  },
  
  // Unfortunately, extending Node.prototype may not work on some browsers,
  // most notably (you've guessed it) IE...
  
  getInnerText : function (node)
  {
    if (node.textContent) return node.textContent;
    if (node.innerText)   return node.innerText;
    var result = "";
    if (node.nodeType == LAPI.DOM.TEXT_NODE) {
      result = node.nodeValue;
    } else {
      Array.forEach (node.childNodes,
        function (elem) {
          switch (elem.nodeType) {
            case LAPI.DOM.ELEMENT_NODE:
              result += LAPI.DOM.getInnerText (elem);
              break;
            case LAPI.DOM.TEXT_NODE:
              result += elem.nodeValue;
              break;
          }
        }
      );
    }
    return result;
  },
  
  removeNode : function (node)
  {
    if (node.parentNode) node.parentNode.removeChild (node);
    return node;
  },

  removeChildren : function (node)
  {
    // if (typeof (node.innerHTML) != 'undefined') node.innerHTML = "";
    // Not a good idea. On IE this destroys all contained nodes, even if they're still referenced
    // from JavaScript! Can't have that...
    while (node.firstChild) node.removeChild (node.firstChild);
    return node;
  },

  insertNode : function (node, before)
  {
    before.parentNode.insertBefore (node, before);
    return node;
  },

  insertAfter : function (node, after)
  {
    var next = after.nextSibling;
    after.parentNode.insertBefore (node, next);
    return node;
  },
  
  replaceNode : function (node, newNode)
  {
    node.parentNode.replaceChild (node, newNode);
    return newNode;
  },
  
  isParentOf : function (parent, child)
  {
    while (child && child != parent && child.parentNode) child = child.parentNode;
    return child == parent;
  },

  // Property is to be in CSS style, e.g. 'background-color', not in JS style ('backgroundColor')!
  // Use standard 'cssFloat' for float property.
  currentStyle : function (element, property)
  {
    function normalize (prop) {
      // Don't use a regexp with a lambda function (available only in JS 1.3)... and I once had a
      // case where IE6 goofed grossly with a lambda function. Since then I try to avoid those
      // (though they're neat).
      if (prop == 'cssFloat') return 'styleFloat'; // We'll try both variants below, standard first...
      var result = prop.split ('-');
      result =
        Array.map (result, function (s) { if (s) return s.capitalizeFirst (); else return s;});
      result = result.join ("");
      return result.lowercaseFirst ();
    }

    if (element.ownerDocument.defaultView
        && element.ownerDocument.defaultView.getComputedStyle)
    { // Gecko etc.
      if (property == 'cssFloat') property = 'float';
      return
        element.ownerDocument.defaultView.getComputedStyle (element, null).getPropertyValue (property);
    } else {
      var result;
      if (element.currentStyle) { // IE, has subtle differences to getComputedStyle
        result = element.currentStyle[property] || element.currentStyle[normalize (property)];
      } else // Not exactly right, but best effort
        result = element.style[property] || element.style[normalize (property)];
      // Convert em etc. to pixels. Kudos to Dean Edwards; see
      // http://erik.eae.net/archives/2007/07/27/18.54.15/#comment-102291
      if (!/^\d+(px)?$/i.test (result) && /^\d/.test (result) && element.runtimeStyle) {
        var style                 = element.style.left;
        var runtimeStyle          = element.runtimeStyle.left;
        element.runtimeStyle.left = element.currentStyle.left;
        element.style.left        = result || 0;
        result = elem.style.pixelLeft + "px";
        element.style.left        = style;
        element.runtimeStyle.left = runtimeStyle;
      }
    }
  },

  // Load a given image in a given size. Parameters:
  //   title
  //     Full title of the image, including the "File:" namespace
  //   url
  //     If != null, URL of an existing thumb for that image. If width is null, may contain the url
  //     of the full image.
  //   width
  //     If != null, desired width of the image, otherwise load the full image
  //   height
  //     If width != null, height should also be set.
  //   auto_thumbs
  //     True if missing thumbnails are generated automatically.
  //   success
  //     Function to be called once the image is loaded. Takes one parameter: the IMG-tag of
  //     the loaded image
  //   failure
  //     Function to be called if the image cannot be loaded. Takes one parameter: a string
  //     containing an error message.
  loadImage : function (title, url, width, height, auto_thumbs, success, failure)
  {
    if (auto_thumbs && url) {
      // MediaWiki-style with 404 handler. Set condition to false if your wiki does not have such a
      // setup.
      var img_src = null;
      if (width) {
        var i = url.lastIndexOf ('/');
        if (i >= 0) {
          img_src = url.substring (0, i)
                  + url.substring (i).replace (/^\/\d+px-/, '/' + width + 'px-');
        }
      } else if (url) {
        img_src = url;
      }
      if (!img_src) {
        failure ("Cannot load image from url " + url);
        return;
      }
      var img_loader =
        LAPI.make (
            'img'
          , {src: img_src}
          , {position: 'absolute', top: '0px', left: '0px', display: 'none'}
        );
      if (width) img_loader.width = "" + width;
      if (height) img_loader.height = "" + height;
      LAPI.Evt.attach (img_loader, 'load', function () {success (img_loader);});
      document.body.appendChild (img_loader); // Now the browser goes loading the image
    } else {
      // No url to work with. Use parseWikitext to have a thumb generated an to get its URL.
      LAPI.Ajax.parseWikitext (
          '[[' + title + (width ? '|' + width + 'px' : "") + ']]'
        , function (html, failureFunc) {
            var dummy =
              LAPI.make (
                  'div'
                , null
                , {position: 'absolute', top: '0px', left: '0px', display: 'none'}
              );
            document.body.appendChild (dummy); // Now start loading the image
            dummy.innerHTML = html;
            var imgs = dummy.getElementsByTagName ('img');
            LAPI.Evt.attach (
                imgs[0], 'load'
              , function () {
                  success (imgs[0]);
                  LAPI.DOM.removeNode (dummy);
                }
            );
          }
        , function (request, json_result)
          {
            failure ("Image loading failed: " + request.status + ' ' + request.statusText);
          }
        , false // Not as preview
        , null  // user language: don't care
        , null  // on page: don't care
        , 3600  // Cache for an hour
      );
    }
  }

}; // end LAPI.DOM

LAPI.DOM.DocumentFacade = function () {this.initialize.apply (this, arguments);};

LAPI.DOM.DocumentFacade.prototype =
{
  initialize : function (text)
  {
    // It's not a real document, but it will behave like one for our purposes.
    this.documentElement = LAPI.make ('div', null, {display: 'none', position: 'absolute'});
    this.body = LAPI.make ('div', null, {position: 'relative'});
    this.documentElement.appendChild (this.body);
    document.body.appendChild (this.documentElement);
    this.body.innerHTML = text;
    // Find all forms
    var forms = document.getElementsByTagName ('form');
    var self = this;
    this.forms = Array.select (forms, function (f) {return LAPI.DOM.isParentOf (self.body, f);});
    // Konqueror 4.2.3/4.2.4 clears form.elements when the containing div is removed from the
    // parent document?!
    if (!LAPI.Browser.is_khtml) {
      LAPI.DOM.removeNode (this.documentElement);
    } else {
      this.dispose = function () {LAPI.DOM.removeNode (this.documentElement);};
      // Since we must leave the stuff *in* the original document on Konqueror, we'll also need a
      // dispose routine... what an ugly hack.
    }
    this.allIDs = {};
    this.isFake = true;
  },

  createElement : function (tag) { return document.createElement (tag); },
  createDocumentFragment : function () { return document.createDocumentFragment (); },
  createTextNode : function (text) { return document.createTextNode (text); },
  createComment : function (text) { return document.createComment (text); },
  createCDATASection : function (text) { return document.createCDATASection (text); },
  createAttribute : function (name) { return document.createAttribute (name); },
  createEntityReference : function (name) { return document.createEntityReference (name); },
  createProcessingInstruction : function (target, data) { return document.createProcessingInstruction (target, data); },
  
  getElementsByTagName : function (tag)
  {
    // Grossly inefficient, but deprecated anyway
    var res = [];
    function traverse (node, tag)
    {
      if (node.nodeName.toLowerCase () == tag) res[res.length] = node;
      var curr = node.firstChild;
      while (curr) { traverse (curr, tag); curr = curr.nextSibling; }
    }
    traverse (this.body, tag.toLowerCase ());
    return res;
  },

  getElementById : function (id)
  {
    function traverse (elem, id)
    {
      if (elem.id == id) return elem;
      var res  = null;
      var curr = elem.firstChild;
      while (curr && !res) {
        res = traverse (curr, id);
        curr = curr.nextSibling;
      }
      return res;
    }
    
    if (!this.allIDs[id]) this.allIDs[id] = traverse (this.body, id);
    return this.allIDs[id];
  }

  // ...NS operations omitted

}; // end DocumentFacade

if (document.importNode) {
  LAPI.DOM.DocumentFacade.prototype.importNode =
    function (node, deep) { document.importNode (node, deep); };
}

} // end if (guard)

if (typeof (LAPI.WP) == 'undefined') {

LAPI.WP = {

  getContentDiv : function (doc)
  {
    // Monobook, modern, classic skins
    return LAPI.$ (['bodyContent', 'mw_contentholder', 'article'], doc);
  },

  fullImageSizeFromPage : function (doc)
  {
    // Get the full img size. This is screenscraping :-( but there are times where you don't
    // want to get this info from the server using an Ajax call.
    // Note: we get the size from the file history table because the text just below the image
    // is all scrambled on RTL wikis. For instance, on ar-WP, it is
    // "‏ (1,806 × 1,341 بكسل، حجم الملف: 996 كيلوبايت، نوع الملف: image/jpeg) and with uselang=en, 
    // it is at ar-WP "‏ (1,806 × 1,341 pixels, file size: 996 KB, MIME type: image/jpeg)"
    // However, in the file history table, it looks good no matter the language and writing
    // direction.
    // Update: this fails on e.g. ar-WP because someone had the great idea to use localized
    // numerals, but the digit transform table is empty!
    var result = {width : 0, height : 0};
    var file_hist = LAPI.$ ('mw-imagepage-section-filehistory', doc);
    if (!file_hist) return result;
    try {
      var file_curr = getElementsByClassName (file_hist, 'td', 'filehistory-selected');
      // Did they change the column order here? It once was nextSibling.nextSibling... but somehow
      // the thumbnails seem to be gone... Right:
      // http://svn.wikimedia.org/viewvc/mediawiki/trunk/phase3/includes/ImagePage.php?r1=52385&r2=53130
      file_hist = LAPI.DOM.getInnerText (file_curr[0].nextSibling);
      if (!file_hist.contains ('×')) {
        file_hist = LAPI.DOM.getInnerText (file_curr[0].nextSibling.nextSibling);
        if (!file_hist.contains ('×')) file_hist = null;
      }
    } catch (ex) {
      return result;
    }
    // Now we have "number×number" followed by something arbitrary 
    if (file_hist) {
      file_hist = file_hist.split ('×', 2);
      result.width  = parseInt (file_hist.shift ().replace (/[^0-9]/g, ""), 10);
      // Height is a bit more difficult because e.g. uselang=eo uses a space as the thousands
      // separator. Hence we have to extract this more carefully
      file_hist = file_hist.pop(); // Everything after the "×"
      // Remove any white space embedded between digits
      file_hist = file_hist.replace (/(\d)\s*(\d)/g, '$1$2');
      file_hist = file_hist.split (" ",2).shift ().replace (/[^0-9]/g, "");
      result.height = parseInt (file_hist, 10);
      if (isNaN (result.width) || isNaN (result.height)) result = {width : 0, height : 0};
    }
    return result;
  },

  getPreviewImage : function (title, doc)
  {
    var file_div = LAPI.$ ('file', doc);
    if (!file_div) return null; // Catch page without file...
    var imgs     = file_div.getElementsByTagName ('img');
    title = title || wgTitle;
    for (var i = 0; i < imgs.length; i++) {
      var src = decodeURIComponent (imgs[i].getAttribute ('src', 2)).replace ('%26', '&');
      if (src.search (new RegExp ('^' + LAPI_file_store + '.*' + title.replace (/ /g, '_').replace (/(\.svg)$/i, '$1.png').escapeRE () + '$')) == 0)
        return imgs[i];
    }
    return null;
  },

  pageFromLink : function (lk)
  {
    if (!lk) return null;
    var href = lk.getAttribute ('href', 2);
    if (!href) return null;
    // This is a bit tricky to get right, because wgScript can be a substring prefix of
    // wgArticlePath, or vice versa.
    var script = wgScript + '?';
    if (href.startsWith (script) || href.startsWith (wgServer + script) || wgServer.startsWith('//') && href.startsWith (document.location.protocol + wgServer + script)) {
      // href="/w/index.php?title=..."
      return href.getParamValue ('title');
    }
    // Now try wgArticlePath: href="/wiki/..."
    var prefix = wgArticlePath.replace ('$1', "");
    if (!href.startsWith (prefix)) prefix = wgServer + prefix; // Fully expanded URL?
    if (!href.startsWith (prefix) && prefix.startsWith ('//')) prefix = document.location.protocol + prefix; // Protocol-relative wgServer?
    if (href.startsWith (prefix))
      return decodeURIComponent (href.substring (prefix.length));
    // Do we have variants?
    if (typeof (wgVariantArticlePath) != 'undefined'
        && wgVariantArticlePath && wgVariantArticlePath.length > 0)
    {
      var re =
        new RegExp (wgVariantArticlePath.escapeRE().replace ('\\$2', "[^\\/]*").replace ('\\$1', "(.*)"));
      var m  = re.exec (href);
      if (m && m.length > 1) return decodeURIComponent (m[m.length-1]);
    }
    // Finally alternative action paths
    if (typeof (wgActionPaths) != 'undefined' && wgActionPaths) {
      for (var i=0; i < wgActionPaths.length; i++) {
        var p = wgActionPaths[i];
        if (p && p.length > 0) {
          p = p.replace('$1', "");
          if (!href.startsWith (p)) p = wgServer + p;
          if (!href.startsWith (p) && p.startsWith('//')) p = document.location.protocol + p;
          if (href.startsWith (p))
            return decodeURIComponent (href.substring (p.length));
        }
      }
    }
    return null;
  },

  revisionFromHtml : function (htmlOfPage)
  {
    var revision_id = null;
    if (window.mediaWiki) { // MW 1.17+
      revision_id = htmlOfPage.match (/(mediaWiki|mw).config.set\(\{.*"wgCurRevisionId"\s*:\s*(\d+),/);
      if (revision_id) revision_id = parseInt (revision_id[2], 10);
    } else { // MW < 1.17
      revision_id = htmlOfPage.match (/wgCurRevisionId\s*=\s*(\d+)[;,]/);
      if (revision_id) revision_id = parseInt (revision_id[1], 10);
    }
    return revision_id;
  }

}; // end LAPI.WP

} // end if (guard)

if (typeof (LAPI.Ajax.doAction) == 'undefined') {

importScript ('MediaWiki:AjaxSubmit.js'); // Legacy code: ajaxSubmit

LAPI.Ajax.getXML = function (request, failureFunc)
{
  var doc = null;
  if (request.responseXML && request.responseXML.documentElement) {
    doc = request.responseXML;
  } else {
    try {
      doc = LAPI.DOM.parse (request.responseText, 'text/xml');
    } catch (ex) {
      if (typeof (failureFunc) == 'function') failureFunc (request, ex);
      doc = null;
    }
  }
  if (doc) {
    try {
      doc = LAPI.DOM.isValid (doc);
    } catch (ex) {
      if (typeof (failureFunc) == 'function') failureFunc (request, ex);
      doc = null;
    }
  }
  return doc;
};

LAPI.Ajax.getHTML = function (request, failureFunc, sanity_check)
{
  // Konqueror sometimes has severe problems with responseXML. It does set it, but getElementById
  // may fail to find elements known to exist.
  var doc = null;
  if (   request.responseXML && request.responseXML.documentElement
      && request.responseXML.documentElement.tagName == 'HTML'
      && (!sanity_check || request.responseXML.getElementById (sanity_check) != null)
     )
  {
    doc = request.responseXML;
  } else {
    try {
      doc = LAPI.DOM.parseHTML (request.responseText, sanity_check);
      if (!doc) throw new Error ('#Could not understand request result');
    } catch (ex) {
      if (typeof (failureFunc) == 'function') failureFunc (request, ex);
      doc = null;
    }
  }
  if (doc) {
    try {
      doc = LAPI.DOM.isValid (doc);
    } catch (ex) {
      if (typeof (failureFunc) == 'function') failureFunc (request, ex);
      doc = null;
    }
  }
  if (doc === null) return doc;
  // We've gotten XML. There is a subtle difference between XML and (X)HTML concerning leading newlines in textareas:
  // XML is required to pass through any whitespace (http://www.w3.org/TR/2004/REC-xml-20040204/#sec-white-space), whereas
  // HTML may or must not (e.g. http://www.w3.org/TR/html4/appendix/notes.html#h-B.3.1, though it is unclear whether that
  // really applies to the content of a textarea, but the draft HTML 5 spec explicitly says that the first newline in a
  // <textarea> is swallowed in HTML:
  // http://www.whatwg.org/specs/web-apps/current-work/multipage/syntax.html#element-restrictions).
  //   Because of the latter MW1.18+ adds a newline after the <textarea> start tag if the value starts with a newline. That
  // solves bug 12130 (leading newlines swallowed), but since XML passes us this extra newline, we might end up adding a
  // leading newline upon each edit.
  //   Let's try to make sure that all textarea's values are as they should be in HTML.
  if (typeof (LAPI.Ajax.getHTML.extraNewlineRE) == 'undefined') {
    // Feature detection. Compare value after parsing with value after .innerHTML.
    LAPI.Ajax.getHTML.extraNewlineRE = null; // Don't know; hence do nothing
    try {
      var testTA = '<textarea id="test">\nTest</textarea>';
      var testString = '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">\n'
                     + '<html xmlns="http://www.w3.org/1999/xhtml" lang="en" dir="ltr">\n'
                     + '<head><title>Test</title></head><body><form>' + testTA + '</form></body>\n'
                     + '</html>';
      var testDoc = LAPI.DOM.parseHTML (testString, 'test');
      var testVal = "" + testDoc.getElementById ('test').value;
      if (testDoc.dispose) testDoc.dispose();
      var testDiv = LAPI.make ('div', null, {display: 'none'});
      document.body.appendChild (testDiv);
      testDiv.innerHTML = testTA;
      if (testDiv.firstChild.value != testVal) {
        LAPI.Ajax.getHTML.extraNewlineRE = /^\r?\n/;
        if (testDiv.firstChild.value != testVal.replace(LAPI.Ajax.getHTML.extraNewlineRE, "")) {
          // Huh? Not the expected difference: go back to "don't know" mode
          LAPI.Ajax.getHTML.extraNewlineRE = null;
        }
      }
      LAPI.DOM.removeNode (testDiv);
    } catch (any) {
      LAPI.Ajax.getHTML.extraNewlineRE = null;
    }
  }
  if (!doc.isFake && LAPI.Ajax.getHTML.extraNewlineRE !== null) {
    // If have a "fake" doc, then we did parse through .innerHTML anyway. No need to fix anything.
    // (Hm. Maybe we should just always use a fake doc?)
    var tas = doc.getElementsByTagName ('textarea');
    for (var i = 0, l = tas.length; i < l; i++) {
      tas[i].value = tas[i].value.replace(LAPI.Ajax.getHTML.extraNewlineRE, "");
    }
  }
  return doc;
};

LAPI.Ajax.get = function (uri, params, success, failure, config)
{
  var original_failure = failure;
  if (!failure || typeof (failure) != 'function') failure = function () {};
  if (!success || typeof (success) != 'function')
    throw new Error ('No success function supplied for LAPI.Ajax.get '
                     + uri + ' with arguments ' + params.toString ());
  var request = LAPI.Ajax.getRequest ();
  if (!request) {
    failure (request);
    return;
  }
  var args = "";
  var question_mark = uri.indexOf ('?');
  if (question_mark) {
    args = uri.substring (question_mark + 1);
    uri  = uri.substring (0, question_mark);
  }
  if (params != null) {
    if (typeof (params) == 'string' && params.length > 0) {
      args += (args.length > 0 ? '&' : "")
            + ((params.charAt (0) == '&' || params.charAt (0) == '?')
                ? params.substring (1)
                : params
              ); // Must already be encoded!
    } else {
      for (var param in params) {
        args += (args.length > 0 ? '&' : "") + param;
        if (params[param] != null) args += '=' + encodeURIComponent (params[param]);
      }
    }
  }
  var method;
  if (uri.startsWith ('//')) uri = document.location.protocol + uri; // Avoid protocol-relative URIs (IE7 bug)
  if (uri.length + args.length + 1 < (LAPI.Browser.is_ie ? 2040 : 4080)) {
    // Both browsers and web servers may have limits on URL length. IE has a limit of 2083 characters
    // (2048 in the path part), and the WMF servers seem to impose a limit of 4kB.
    method = 'GET'; uri += '?' + args; args = null;
  } else {
    method = 'POST'; // We'll lose caching, but at least we can make the request.
  }
  request.open (method, uri, true);
  request.setRequestHeader ('Pragma', 'cache=yes');
  request.setRequestHeader (
      'Cache-Control'
    , 'no-transform'
      + (params && params.maxage ? ', max-age=' + params.maxage : "")
      + (params && params.smaxage ? ', s-maxage=' + params.smaxage : "")
  );
  if (config) {
    for (var conf in config) {
      if (conf == 'overrideMimeType') {
        if (config[conf] && config[conf].length > 0 && request.overrideMimeType)
          request.overrideMimeType (config[conf]);
      } else {
        request.setRequestHeader (conf, config[conf]);
      }      
    }
  }
  if (args) request.setRequestHeader ('Content-Type', 'application/x-www-form-urlencoded');
  request.onreadystatechange =
    function ()
    {
      if (request.readyState != 4) return; // Wait until the request has completed.
      try {
        if (request.status != 200)
          throw new Error
            ('#Request to server failed. Status: ' + request.status + ' ' + request.statusText
             + ' URI: ' + uri);
        if (!request.responseText)
          throw new Error ('#Empty response from server for request ' + uri);
      } catch (ex) {
        failure (request, ex);
        return;
      }
      success (request, original_failure);
    };
  request.send (args);
};

LAPI.Ajax.getPage = function (page, action, params, success, failure)
{
  var uri = wgServer + wgScript + '?title=' + encodeURIComponent (page)
        + (action ? '&action=' + action : "");
  LAPI.Ajax.get (uri, params, success, failure, {overrideMimeType : 'application/xml'});
};

// modify is supposed to save the changes at the end, e.g. using LAPI.Ajax.submit.
// modify is called with three parameters: the document, possibly the form, and the optional
// failure function. The failure function is called with the request as the first parameter,
// and possibly an exception as the second parameter.
LAPI.Ajax.doAction = function (page, action, form, modify, failure)
{
  if (!page || !action || !modify || typeof (modify) != 'function')
    throw new Error ('Parameter inconsistency in LAPI.Ajax.doAction.');
  var original_failure = failure;
  if (!failure || typeof (failure) != 'function') failure = function () {};
  LAPI.Ajax.getPage (
      page, action, null // No additional parameters
    , function (request, failureFunc) {
        var doc         = null;
        var the_form    = null;
        var revision_id = null;
        try {
          // Convert responseText into DOM tree.
          doc = LAPI.Ajax.getHTML (request, failureFunc, form);      
          if (!doc) return;
          var err_msg = LAPI.$ ('permissions-errors', doc);
          if (err_msg) throw new Error ('#' + LAPI.DOM.getInnerText (err_msg));
          if (form) {
            the_form = LAPI.$ (form, doc);
            if (!the_form) throw new Error ('#Server reply does not contain mandatory form.');
          }
          revision_id = LAPI.WP.revisionFromHtml (request.responseText);
        } catch (ex) {
          failureFunc (request, ex);
          return;
        }
        modify (doc, the_form, original_failure, revision_id)
      }
    , failure
  );
}; // end LAPI.Ajax.doAction
  
LAPI.Ajax.submit = function (form, after_submit)
{
  try {
    ajaxSubmit (form, null, after_submit, true); // Legacy code from MediaWiki:AjaxSubmit
  } catch (ex) {
    after_submit (null, ex);
  }
}; // end LAPI.Ajax.submit

LAPI.Ajax.editPage = function (page, modify, failure)
{
  LAPI.Ajax.doAction (page, 'edit', 'editform', modify, failure);
}; // end LAPI.Ajax.editPage
  
LAPI.Ajax.checkEdit = function (request)
{
  if (!request) return true;
  // Check for previews (session token lost?) or edit forms (edit conflict). 
  try {
    var doc = LAPI.Ajax.getHTML (request, function () {throw new Error ('Cannot check HTML');});
    if (!doc) return false;
    return LAPI.$ (['wikiPreview', 'editform'], doc) == null;
  } catch (anything) {
    return false;
  }
}; // end LAPI.Ajax.checkEdit
  
LAPI.Ajax.submitEdit = function (form, success, failure)
{
  if (!success || typeof (success) != 'function') success = function () {};
  if (!failure || typeof (failure) != 'function') failure = function () {};
  LAPI.Ajax.submit (
      form
    , function (request, ex)
      {
        if (ex) {
          failure (request, ex);
        } else {
          var successful = false;
          try {
            successful = request && request.status == 200 && LAPI.Ajax.checkEdit (request);
          } catch (some_error) {
            failure (request, some_error);
            return;
          }
          if (successful)
            success (request);
          else
            failure (request);
        }
      }
  );
}; // end LAPI.Ajax.submitEdit

LAPI.Ajax.apiGet = function (action, params, success, failure)
{
  var original_failure = failure;
  if (!failure || typeof (failure) != 'function') failure = function () {};
  if (!success || typeof (success) != 'function')
    throw new Error ('No success function supplied for LAPI.Ajax.apiGet '
                     + action + ' with arguments ' + params.toString ());
  var is_json = false;
  if (params != null) {
    if (typeof (params) == 'string') {
      if (!/format=[^&]+/.test (params)) params += '&format=json';
      is_json = /format=json(&|$)/.test (params); // Exclude jsonfm, which actually serves XHTML
    } else {
      if (typeof (params['format']) != 'string' || params.format.length == 0) params.format = 'json';
      is_json = params.format == 'json';
    }
  }
  var uri = wgServer + wgScriptPath + '/api.php' + (action ? '?action=' + action : "");
  LAPI.Ajax.get (
      uri, params
    , function (request, failureFunc) {
        if (is_json && request.responseText.trimLeft().charAt (0) != '{') {
          failureFunc (request);
        } else {
          success (
              request
            , (is_json ? eval ('(' + request.responseText.trimLeft() + ')') : null)
            , original_failure
          );
        }
      }
    , failure
  );
}; // end LAPI.Ajax.apiGet

LAPI.Ajax.parseWikitext = function (wikitext, success, failure, as_preview, user_language, on_page, cache)
{
  if (!failure || typeof (failure) != 'function') failure = function () {};
  if (!success || typeof (success) != 'function')
    throw new Error ('No success function supplied for parseWikitext');
  if (!wikitext && !on_page)
    throw new Error ('No wikitext or page supplied for parseWikitext');
  var params = null;
  if (!wikitext) {
    params = {pst: null, page: on_page};
  } else {
    params =
      { pst  : null // Do the pre-save-transform: Pipe magic, tilde expansion, etc.
       ,text :
          (as_preview ? '\<div style="border:1px solid red; padding:0.5em;"\>'
                        + '\<div class="previewnote"\>'
                        + '\{\{MediaWiki:Previewnote/' + (user_language || wgUserLanguage) +'\}\}'
                        + '\<\/div>\<div\>\n'
                      : "")
          + wikitext
          + (as_preview ? '\<\/div\>\<div style="clear:both;"\>\<\/div\>\<\/div\>' : "")
        ,title: on_page || wgPageName || "API"
      };
  }
  params.prop    = 'text';
  params.uselang = user_language || wgUserLanguage; // see bugzilla 22764
  if (cache && /^\d+$/.test(cache=cache.toString())) {
    params.maxage = cache;
    params.smaxage = cache;
  }
  LAPI.Ajax.apiGet (
      'parse'
    , params
    , function (req, json_result, failureFunc)
      {
        // Success.
        if (!json_result || !json_result.parse || !json_result.parse.text) {
          failureFunc (req, json_result);
          return;
        }
        success (json_result.parse.text['*'], failureFunc);
      }
    , failure
  );
}; // end LAPI.Ajax.parseWikitext

LAPI.Ajax.injectSpinner = injectSpinner;
LAPI.Ajax.removeSpinner = removeSpinner;

} // end if (guard)

if (typeof (LAPI.Pos) == 'undefined') {
  
LAPI.Pos =
{
  // Returns the global coordinates of the mouse pointer within the document.
  mousePosition : function (evt)
  {
    if (!evt || (typeof (evt.pageX) == 'undefined' && typeof (evt.clientX) == 'undefined'))
      // No way to calculate a mouse pointer position
      return null;
    if (typeof (evt.pageX) != 'undefined')
      return { x : evt.pageX, y : evt.pageY };
      
    var offset      = LAPI.Pos.scrollOffset ();
    var mouse_delta = LAPI.Pos.mouse_offset ();
    var coor_x = evt.clientX + offset.x - mouse_delta.x;
    var coor_y = evt.clientY + offset.y - mouse_delta.y;
    return { x : coor_x, y : coor_y };
  },
  
  // Operations on document level:
  
  // Returns the scroll offset of the whole document (in other words, the coordinates
  // of the top left corner of the viewport).
  scrollOffset : function ()
  {
    return {x : LAPI.Pos.getScroll ('Left'), y : LAPI.Pos.getScroll ('Top') };
  },
  
  getScroll : function (what)
  {
    var s = 'scroll' + what;
    return (document.documentElement ? document.documentElement[s] : 0)
           || document.body[s] || 0;
  },
  
  // Returns the size of the viewport (result.x is the width, result.y the height).
  viewport : function ()
  {
    return {x : LAPI.Pos.getViewport ('Width'), y : LAPI.Pos.getViewport ('Height') };
  },
  
  getViewport : function (what)
  {
    if (   LAPI.Browser.is_opera_95 && what == 'Height'
        || LAPI.Browser.is_safari && !document.evaluate)
      return window['inner' + what];
    var s = 'client' + what;
    if (LAPI.Browser.is_opera) return document.body[s];
    return (document.documentElement ? document.documentElement[s] : 0)
           || document.body[s] || 0;
  },
  
  // Operations on DOM nodes
  
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
        var scroll = LAPI.Pos.scrollOffset ();
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
        top  += parseInt (LAPI.DOM.currentStyle (body, 'margin-top'), 10) || 0;
        left += parseInt (LAPI.DOM.currentStyle (body, 'margin-left'), 10) || 0;
      }
      return {x: left, y: top};
    }

    return jQuery_offset;
  })(),

  isWithin : function (node, x, y)
  {
    if (!node || !node.parentNode) return false;
    var pos = LAPI.Pos.position (node);
    return    (x == null || x > pos.x && x < pos.x + node.offsetWidth)
           && (y == null || y > pos.y && y < pos.y + node.offsetHeight);
  },
  
  // Private:
  
  // IE has some strange offset...
  mouse_offset : function ()
  {
    if (LAPI.Browser.is_ie) {
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
    return {x: 0, y : 0};
  }

}; // end LAPI.Pos

} // end if (guard)

if (typeof (LAPI.Evt) == 'undefined') {
  
LAPI.Evt =
{
  listenTo : function (object, node, evt, f, capture)
  {
    var listener = LAPI.Evt.makeListener (object, f);
    LAPI.Evt.attach (node, evt, listener, capture);
  },
 
  attach : function (node, evt, f, capture)
  {
    if (node.attachEvent) node.attachEvent ('on' + evt, f);
    else if (node.addEventListener) node.addEventListener (evt, f, capture);
    else node['on' + evt] = f;
  },
 
  remove : function (node, evt, f, capture)
  {
    if (node.detachEvent) node.detachEvent ('on' + evt, f);
    else if (node.removeEventListener) node.removeEventListener (evt, f, capture);
    else node['on' + evt] = null;
  },
 
  makeListener : function (obj, listener)
  {
    // Some hacking around to make sure 'this' is set correctly
    var object = obj, f = listener;
    return function (evt) { return f.apply (object, [evt || window.event]); }
    // Alternative implementation:
    // var f = listener.bind (obj);
    // return function (evt) { return f (evt || window.event); };
  },

  kill : function (evt)
  {
    if (typeof (evt.preventDefault) == 'function') {
      evt.stopPropagation ();
      evt.preventDefault (); // Don't follow the link
    } else if (typeof (evt.cancelBubble) != 'undefined') { // IE...
      evt.cancelBubble = true;
    }
    return false; // Don't follow the link (IE)
  }

}; // end LAPI.Evt

} // end if (guard)

if (typeof (LAPI.Edit) == 'undefined') {

LAPI.Edit = function () {this.initialize.apply (this, arguments);};

LAPI.Edit.SAVE    = 1;
LAPI.Edit.PREVIEW = 2;
LAPI.Edit.REVERT  = 4;
LAPI.Edit.CANCEL  = 8;

LAPI.Edit.prototype =
{
  initialize : function (initial_text, columns, rows, labels, handlers)
  {
    var my_labels =
      {box : null, preview : null, save : 'Save', cancel : 'Cancel', nullsave : null, revert : null, post: null};
    if (labels) my_labels = Object.merge (labels, my_labels);
    this.labels = my_labels;
    this.timestamp = (new Date ()).getTime ();
    this.id = 'simpleedit_' + this.timestamp;
    this.view = LAPI.make ('div', {id : this.id}, {marginRight: '1em'});
    // Somehow, the textbox extends beyond the bounding box of the view. Don't know why, but
    // adding a small margin fixes the layout more or less.
    this.form =
      LAPI.make (
          'form'
        , { id      : this.id + '_form'
           ,action  : ""
           ,onsubmit: (function () {})
          }
      );
    if (my_labels.box) {
      var label = LAPI.make ('div');
      label.appendChild (LAPI.DOM.makeLabel (this.id + '_label', my_labels.box, this.id + '_text'));
      this.form.appendChild (label);
    }
    this.textarea =
      LAPI.make (
          'textarea'
        , { id   : this.id + '_text'
           ,cols : columns
           ,rows : rows
           ,value: (initial_text ? initial_text.toString () : "")
          }
      );
    LAPI.Evt.attach (this.textarea, 'keyup', LAPI.Evt.makeListener (this, this.text_changed));
    // Catch cut/copy/paste through the context menu. Some browsers support oncut, oncopy,
    // onpaste events for this, but since that's only IE, FF 3, Safari 3, and Chrome, we
    // cannot rely on this. Instead, we check again as soon as we leave the textarea. Only
    // minor catch is that on FF 3, the next focus target is determined before the blur event
    // fires. Since in practice save will always be enabled, this shouldn't be a problem.
    LAPI.Evt.attach (this.textarea, 'mouseout', LAPI.Evt.makeListener (this, this.text_changed));
    LAPI.Evt.attach (this.textarea, 'blur', LAPI.Evt.makeListener (this, this.text_changed));
    this.form.appendChild (this.textarea);
    this.form.appendChild (LAPI.make ('br'));
    this.preview_section =
      LAPI.make ('div', null, {borderBottom: '1px solid #8888aa', display: 'none'});
    this.view.insertBefore (this.preview_section, this.view.firstChild);
    this.save =
      LAPI.DOM.makeButton
        (this.id + '_save', my_labels.save, LAPI.Evt.makeListener (this, this.do_save));
    this.form.appendChild (this.save);
    if (my_labels.preview) {
      this.preview =
        LAPI.DOM.makeButton
          (this.id + '_preview', my_labels.preview, LAPI.Evt.makeListener (this, this.do_preview));
      this.form.appendChild (this.preview);
    }
    this.cancel =
      LAPI.DOM.makeButton
        (this.id + '_cancel', my_labels.cancel, LAPI.Evt.makeListener (this, this.do_cancel));
    this.form.appendChild (this.cancel);
    this.view.appendChild (this.form);
    if (my_labels.post) {
      this.post_text = LAPI.DOM.setContent (LAPI.make ('div'), my_labels.post);
      this.view.appendChild (this.post_text);
    }
    if (handlers) Object.merge (handlers, this);
    if (typeof (this.ongettext) != 'function')
      this.ongettext = function (text) { return text;}; // Default: no modifications
    this.current_mask = LAPI.Edit.SAVE + LAPI.Edit.PREVIEW + LAPI.Edit.REVERT + LAPI.Edit.CANCEL;
    if ((!initial_text || initial_text.trim ().length == 0) && this.preview)
      this.preview.disabled = true;
    if (my_labels.revert) {
      this.revert = 
        LAPI.DOM.makeButton
          (this.id + '_revert', my_labels.revert, LAPI.Evt.makeListener (this, this.do_revert));
      this.form.insertBefore (this.revert, this.cancel);
    }
    this.original_text = "";
  },
  
  getView : function ()
  {
    return this.view;
  },
  
  getText : function ()
  {
    return this.ongettext (this.textarea.value);
  },
  
  setText : function (text)
  {
    this.textarea.value = text;
    this.original_text  = text;
    this.text_changed ();
  },
  
  changeText : function (text)
  {
    this.textarea.value = text;
    this.text_changed ();
  },

  hidePreview : function ()
  {
    this.preview_section.style.display = 'none';
    if (this.onpreview) this.onpreview (this);
  },

  showPreview : function ()
  {
    this.preview_section.style.display = "";
    if (this.onpreview) this.onpreview (this);
  },

  setPreview : function (html)
  {
    if (html.nodeName) {
      LAPI.DOM.removeChildren (this.preview_section);
      this.preview_section.appendChild (html);
    } else {
      this.preview_section.innerHTML = html;
    }
  },
  
  busy : function (show)
  {
    if (show)
      LAPI.Ajax.injectSpinner (this.cancel, this.id + '_spinner');
    else
      LAPI.Ajax.removeSpinner (this.id + '_spinner');
  },
  
  do_save : function (evt)
  {
    if (this.onsave) this.onsave (this);
    return true;
  },
  
  do_revert : function (evt)
  {
    this.changeText (this.original_text);
    return true;
  },

  do_cancel : function (evt)
  {
    if (this.oncancel) this.oncancel (this);
    return true;
  },
  
  do_preview : function (evt)
  {
    var self = this;
    this.busy (true);
    LAPI.Ajax.parseWikitext (
        this.getText ()
      , function (text, failureFunc)
        {
          self.busy (false);
          self.setPreview (text);
          self.showPreview ();
        }
      , function (req, json_result)
        {
          // Error. TODO: user feedback?
          self.busy (false);
        }
      , true
      , wgUserLanguage || null
      , wgPageName || null
    );
    return true;
  },

  enable : function (bit_set)
  {
    var call_text_changed = false;
    this.current_mask = bit_set;
    this.save.disabled = ((bit_set & LAPI.Edit.SAVE) == 0);
    this.cancel.disabled = ((bit_set & LAPI.Edit.CANCEL) == 0);
    if (this.preview) {
      if ((bit_set & LAPI.Edit.PREVIEW) == 0)
        this.preview.disabled = true;
      else
        call_text_changed = true;
    }
    if (this.revert) {
      if ((bit_set & LAPI.Edit.REVERT) == 0)
        this.revert.disabled = true;
      else
        call_text_changed = true;
    }
    if (call_text_changed) this.text_changed ();
  },

  text_changed : function (evt)
  {
    var text = this.textarea.value;
    text = text.trim ();
    var length = text.length;   
    if (this.preview && (this.current_mask & LAPI.Edit.PREVIEW) != 0) {
      // Preview is basically enabled
      this.preview.disabled = (length <= 0);
    }
    if (this.labels.nullsave) {
      if (length > 0) {
        this.save.value = this.labels.save;
      } else {
        this.save.value = this.labels.nullsave;
      }
    }
    if (this.revert) {
      this.revert.disabled =
        (text == this.original_text || this.textarea.value == this.original_text);
    }
    return true;
  }

}; // end LAPI.Edit

} // end if (guard)

// </source>