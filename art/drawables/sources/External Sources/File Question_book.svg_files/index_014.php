// ajaxSubmit
//   Submit a form through Ajax. Doesn't handle file uploads yet.
//
// Parameters:
//   form                 DOM element   The form to submit
//   button      optional DOM element   If set and a submit button of 'form', is added to the
//                                      form arguments sent
//   func        optional Function      Function to call once the call has been made or the
//                                      result has arrived, if want_result == true
//   want_result optional Boolean       If true, call func with the result of the submit once
//                                      it has arrived. Otherwise, call func as soon as the
//                                      submit request has been received by the server, and
//                                      ignore any result of the submit.
//
// Notes:
//   Func should be a function (request). If func is not defined,
//   ajaxSubmit just submits the form and ignores any result.
function ajaxSubmit (form, button, func, want_result)
{
  
  if (want_result && (!func || typeof (func) != 'function' || func.length < 1)) {
    /**** TODO: improve error handling: should throw an exception! */
    alert (  'Logic error in ajaxSubmit: func must be function (request).');
    return;
  }
  if (func && typeof (func) != 'function') {
    /**** TODO: improve error handling: should throw an exception! */
    alert ('Error in ajaxSubmit: func must be a function, found a ' + typeof (func) + '.');
    return;
  }

  var is_simple = false;
  // True if it's a GET request, or if the form is 'application/x-www-form-urlencoded'
  var boundary  = null; 
  // Otherwise, it's 'multipart/form-data', and the multipart delimiter is 'boundary'
  
  function encode_entry (name, value)
  {
    if (!name || name.length == 0 || !value || value.length == 0) return null;
    if (!boundary)
      return name + '=' + encodeURIComponent (value);
    else
      return boundary + '\r\n'
           + 'Content-Disposition: form-data; name="' + name + '"\r\n'
           + '\r\n'
           + value.replace(/\r?\n/g, '\r\n') + '\r\n'; // RFC 2046: newlines always must be represented as CR-LF
  }
  
  function encode_field (element)
  {
    var name = element.name;
    if (!name || name.length == 0) name = element.id;
    return encode_entry (name, element.value);
  }
  
  function form_add_argument (args, field)
  {
    if (!field || field.length == 0) return args;
    if (!args || args.length == 0) return field;
    if (is_simple)
      return args + '&' + field;
    else
      return args + field;
  }
  
  var request;
  if (window.LAPI && window.LAPI.Ajax && window.LAPI.Ajax.getRequest) {
    request = window.LAPI.Ajax.getRequest();
  } else {
    try {
      request = new window.XMLHttpRequest();
    } catch (anything) {
      if (window.ActiveXObject) request = new window.ActiveXObject('Microsoft.XMLHTTP');
    }
  }
  var method    = form.getAttribute ('method').toUpperCase ();
  var uri       = form.getAttribute ('action');
  if (uri.length >= 2 && uri.substring(0, 2) == '//') {
    // Protocol-relative URI; can cause trouble on IE7
    uri = document.location.protocol + uri;
  } else if (uri.charAt (0) == '/') {
    // Some browsers already expand the action URI (e.g. Opera 9.26)
    uri = wgServer + uri;
    if (uri.length >= 2 && uri.substring(0, 2) == '//') uri = document.location.protocol + uri;
  }
  // Encode the field values

  var is_get    = method == 'GET';
  var encoding  = form.getAttribute ('enctype');
  if (encoding != null) {
    encoding = encoding.toLowerCase ();
    if (encoding.length == 0) encoding = null;
  }
  is_simple =
    is_get || encoding == null || encoding == 'application/x-www-form-urlencoded';

  var args            = "";
  var boundary_string = '----' + wgArticleId+wgCurRevisionId + 'auto_submit_by_lupo';

  boundary = null;
  
  if (!is_simple) boundary = '--' + boundary_string;

  for (var i = 0; i < form.elements.length; i++) {
    var element       = form.elements[i];
    var single_select = false;
    switch (element.type) {
      case 'checkbox':
      case 'radio':
        if (!element.checked) break;
        // else fall-through
      case 'hidden':
      case 'text':
      case 'password':
      case 'textarea':
        args = form_add_argument (args, encode_field (element));
        break;
      case 'select-one':
        single_select = true;
        // fall-through
      case 'select-multiple':
        var name = element.name || element.id || "";
        if (name.length == 0) break;
        for (var j = 0; j < element.length; j++) {
          if (element[j].selected) {
            var value = element[j].value || element[j].text;
            args = form_add_argument (args, encode_entry (name, value));
            if (single_select) break; // No need to scan the rest
          }
        }
        break;
      case 'file':
        break;
    }
  }
  if (button && button.form == form && button.type == 'submit')
    args = form_add_argument (args, encode_field (button));
    
  // Close the multipart request
  if (!is_simple && args.length > 0) args = args + boundary;

  if (method == 'GET') {
    uri = uri + (uri.indexOf('?') < 0 ? '?' : '&') + args; args = null;
  }
  // Make the request
  request.open (method, uri, true);
  if (want_result && request.overrideMimeType) request.overrideMimeType ('application/xml');
  request.setRequestHeader ('Pragma', 'cache=no');
  request.setRequestHeader ('Cache-Control', 'no-transform');
  if (method == 'POST') {
    if (encoding == null) encoding = 'application/x-www-form-urlencoded';
    if (!is_simple) {
      request.setRequestHeader (
        'Content-type', encoding + '; charset=UTF-8; boundary="' + boundary_string + '"');
    } else {
      request.setRequestHeader ('Content-type', encoding);
    }
    try {
      request.setRequestHeader ('Content-length', args.length);
    } catch (anything) {
      // Just swallow. Some browsers don't like setting this but prefer to do it themselves.
      // Safari 4 for instance issues an "error" about an "unsafe" setting not done, but then
      // continues anyway. The exception handler here is just paranoia in case someone decides
      // to make this a hard error.
    }
  }
  request.onreadystatechange =
    function() {
      if (want_result) {
        if (request.readyState < 4) return;
        func (request);
      } else {
        // Call func as soon as the request has been sent and we start getting the result.
        if (request.readyState == 3 && func) func (request);
      }
    }
  request.send (args);
}

// submitAndClose
//   Submit a form and close the window containing it as soon as the request has been
//   received by the server
//
// Parameters:
//   form   DOM element   The form to submit.
function submitAndClose (form)
{
  ajaxSubmit (form, null, function () { window.close (); });
}