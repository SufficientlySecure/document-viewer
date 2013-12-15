// <source lang="javascript">
/*
  Read UI elements from the DOM. Used by the upload form rewrite.
 
  Author: [[User:Lupo]], March 2008
  License: Quadruple licensed GFDL, GPL, LGPL and Creative Commons Attribution 3.0 (CC-BY-3.0)
 
  Choose whichever license of these you like best :-)
*/

var UIElements = {

  defaultLanguage : 'en',

  getElementsByClassName : function (elem, tag, classes) {
    // getElementsByClassName in wikibits.js has been changed in a contract-breaking way and
    // newly won't work anymore with regexp strings or arrays of strings passed as classes.
    // We need this functionality here, so we have our own copy of this function.
    var arrElements = (tag == "*" && elem.all) ? elem.all : elem.getElementsByTagName (tag);
    var arrReturnElements = new Array();
    var arrRegExpClassNames = new Array();

    if (typeof classes == "object") {
      for (var i=0; i < classes.length; i++){
        arrRegExpClassNames[arrRegExpClassNames.length] =
          new RegExp("(^|\\s)" + classes[i].replace(/\-/g, "\\-") + "(\\s|$)");
      }
    } else {
      arrRegExpClassNames[arrRegExpClassNames.length] =
        new RegExp("(^|\\s)" + classes.replace(/\-/g, "\\-") + "(\\s|$)");
    }

    var this_elem;
    var matches_all;

    for (var j=0; j < arrElements.length; j++){
      this_elem = arrElements[j];
      matches_all = true;
      for (var k=0; k < arrRegExpClassNames.length; k++){
        if (!arrRegExpClassNames[k].test (this_elem.className)) {
          matches_all = false;
          break;
        }
      }
      if (matches_all) {
        arrReturnElements[arrReturnElements.length] = this_elem;
      }
    }
    return (arrReturnElements)
  },
  
  load : function (container_name, items, tag, repository, default_language, doc)
  { 
    doc = doc || document;

    function add_item (item, lang)
    {
      var classes = item.className.split (' ');
      var node = null;
      if (item.ownerDocument != document && document.importNode)
        node = document.importNode (item, true);
      else
        node = item.cloneNode (true);
      UIElements.setEntry (classes[0], repository, node, lang, classes.slice (1));
    }
    
    if (!container_name || container_name.length == 0 || container_name == '*') return repository;
    var base = getElementsByClassName (doc, 'div', container_name);
    if (!base || base.length == 0) return repository;
    
    if (!items || items.length == 0)
      items = ['wp\\S*'];
    else if (items && typeof (items) == 'string')
      items = [items];
    if (!repository) repository = UIElements.emptyRepository (default_language);
    
    for (var i = 0; i < base.length; i++) {
      var b = base[i];
      var lang = (b.getAttribute ('lang') || repository.defaultLanguage).replace(/-/g, '_');
      for (var j = 0; j < items.length; j++) {
        var nodes = UIElements.getElementsByClassName (b, tag || '*', items[j]);
        if (nodes) {
          for (var k = 0; k < nodes.length; k++) add_item (nodes[k], lang);
        }
      }
    }
    return repository;
  },
  
  getEntry : function (id, repository, lang, selector)
  {
    if (repository && repository[id]) {
      if (!lang || lang.length == 0)
        lang = repository.defaultLanguage || UIElements.defaultLanguage;
      lang = lang.replace (/-/g, '_');
      if (repository[id][lang]) {
        if (selector == '*')
          return repository[id][lang];
        else if (!selector || selector.length == 0)
          return repository[id][lang]._default;
        else
          return repository[id][lang][selector.replace (/-/g, '_')];
      }
    }
    return null;
  },
  
  setEntry : function (id, repository, value, lang, selectors)
  {
    if (!repository) return null;
    if (!lang) lang = repository.defaultLanguage;
    lang = lang.replace (/-/g, '_');
    id   = id.replace(/-/g, '_');
    if (!repository[id]) repository[id] = new Object ();
    if (!repository[id][lang]) repository[id][lang] = new Object ();
    if (!selectors || selectors.length == 0)
      repository[id][lang]._default = value;
    else {
      for (var k = 0; k < selectors.length; k++) {
        if (!selectors[k] || selectors[k].length == 0)
          repository[id][lang]._default = value;
        else
          repository[id][lang][selectors[k].replace (/-/g, '_')] = value;
      }
    }
  },
  
  emptyRepository : function (default_language)
  {
    var repository = new Object ();
    repository.defaultLanguage = default_language || UIElements.defaultLanguage;
    return repository;
  }
  
} // end UIElements
// </source>