// <source lang="javascript">
/*
  Wikitext sanitation for MediaWiki
 
  Author: [[User:Lupo]], January 2008
  License: Quadruple licensed GFDL, GPL, LGPL and Creative Commons Attribution 3.0 (CC-BY-3.0)
 
  Choose whichever license of these you like best :-)
*/

var TextCleaner =
{
  
  imgNamespaceNames : null,

  // This function attempts to construct well-formed wikitext from input that may contain
  // possibly broken wikitext.
  //
  // Note: even just a half-baked sanitation of wikitext is hyper-complex due to the presence
  // of templates, and due to the fact that image thumbnail captions may themselves contain
  // links. This implementation catches the most common errors (such as forgetting to close a
  // template or a link), and even some more elaborate ones. With enough malice, this sanitation
  // can still be broken by user input such that the result is not well-formed wikitext as the
  // parser at the servers would like to have it. (It's still possible that the result is broken
  // wikitext, if the input was broken wikitext. But it never transforms well-formed wikitext
  // into broken wikitext.)
  //
  // If 'only_thumbs' is true, all [[Image: links are changed to [[:Image:, unless the original
  // image link was a thumbnail or had a width smaller than 300px specified.
  //
  // WARNING: do *not* attempt to use this to process large texts (e.g., a whole article). It is
  // probably rather inefficient due to the many substrings that are generated. This function is
  // primarily intended to be used to clean up user input in forms, which are typically rather
  // short.
  sanitizeWikiText : function (input, only_thumbs)
  {
    if (input.search (/[\][}{]|<nowiki(\s[^>]*)?>|<\!--/) < 0) return input;
    // No critical characters
    
    if (!TextCleaner.imgNamespaceNames) {
      TextCleaner.imgNamespaceNames = [];
      if (wgNamespaceIds) {
        for (name in wgNamespaceIds) {
          if (wgNamespaceIds[name] == 6) // Image namespace
            TextCleaner.imgNamespaceNames[TextCleaner.imgNamespaceNames.length] = name;
        }
      }
      // Make sure that we have the two canonical names
      TextCleaner.imgNamespaceNames[TextCleaner.imgNamespaceNames.length] = 'Image';
      TextCleaner.imgNamespaceNames[TextCleaner.imgNamespaceNames.length] = 'File';
      // If your Wiki does not have wgNamespaceIds, add aliases or localized namespace names here!
    }

    var consumed       = new Array (0, 0);
    // For image captions. Image caption may contain links, and may even contain images.
    // The current MediaWiki parser actually allows this only once. For deeper recursions,
    // it fails. But here, it's actually easier to implement no limit.
 
    var base_regexp    =
      new RegExp
            (   "[\\x01\\x02\\x03\\x04[\\]\\|\\x05\\x06\\x07\\x08]"
              + "|\<nowiki(\\s[^>]*)?\>|\<\!--"
            , "i"); // Ignore case
    var nowiki_regexp  = new RegExp ("\<\\/nowiki(\\s[^>]*)?\>|\<\!--", "i");
    
    var allow_only_thumbs = only_thumbs;

    function sanitize
      (s, with_links, caption_level, allow_thumbs, break_at_pipe, with_tables, with_galleries)
    {
      if (!s || s.length == 0) {
        if (caption_level > 0) {
          if (consumed.length < caption_level)
            consumed.push (0);
          else
            consumed[caption_level-1] = 0;
        }
        return s;
      }
      
      var result         = "";
      var initial_length = s.length;
      var get_out        = false;
      var in_nowiki      = false;
      var endings        = null;
      // Stack recording template and table nesting
      var next;
      
      function push_end (val)
      {
        if (endings == null) {
          endings = new Array (1);
          endings[0] = val;
        } else {
          endings[endings.length] = val;
        }
      }

      function pop_end ()
      {
        if (endings == null) return null; // Shouldn't happen
        var result;
        if (endings.length == 1) {
          result = endings[0];
          endings = null;
        } else {
          result = endings[endings.length -1];
          endings.length = endings.length - 1;
        }
        return result;
      }
          
      regexp = base_regexp;
      while (s.length > 0 && !get_out) {
        next = s.search (regexp);
      
        if (next < 0) {
          result = result + s;
          break;
        }
        var ch = s.charAt (next);
        var i  = -1;
        var j  = -1;
        var k  = -1;
        switch (ch) {
          case '<':
            // Nowiki or HTML comment. Must be closed.
            if (s.charAt (next+1) == '!') {
              // HTML comment. Cannot be nested.
              i = s.indexOf ('--\>', next+3);
              if (i < 0) {
                result = result + s + '--\>';
                s = "";
              } else {
                result = result + s.substring (0, i + 3);
                s = s.substring (i+3);
              }
            } else if (s.charAt (next+1) == 'n') {
              // Nowiki may contain HTML comments!
              in_nowiki = true;
              regexp = nowiki_regexp;
              result = result + s.substring (0, next + 7);
              s = s.substring (next + 7);
            } else {
              // End of nowiki. Searched for and found only if in_nowiki == true
              in_nowiki = false;
              regexp = base_regexp;
              i = s.indexOf ('>', next+1); // End of tag
              result = result + s.substring (0, i+1);
              s = s.substring (i+1);
            }
            break;
          case '\x05':
            // Table start
            if (!with_tables) {
              result  = result + s.substring (0, next);
              get_out = true;
              break;
            }
            // Fall through
          case '\x07':
            if (ch == '\x07' && !with_galleries) {
              result = result + s.substring (0, next);
              get_out = true;
              break;
            }
          case '\x01':
            // Start of template, table, or gallery
            result = result + s.substring (0, next+1);
            push_end (String.fromCharCode(ch.charCodeAt (0)+1).charAt (0));
            s = s.substring (next+1);
            break;
          case '\x06':
            // Table end
            if (break_at_pipe && endings == null) {
              result = result + s.substring (0, next);
              get_out = true;
              break;
            }
            // Fall through
          case '\x02':
            // End of a template or table
            result = result + s.substring (0, next);
            if (endings == null || endings[endings.length - 1] != ch) {
              // Spurious template or table end
              if (ch == '\x02')
                result = result + '&#x7D;&#x7D;';
              else
                result = result + '&#x7C;&#x7D;';
            } else {            
              result = result + pop_end ();
            }
            s = s.substring (next+1);
            break;
          case '\x08':
            // End of gallery
            result = result + s.substring (0, next+1);
            if (endings != null && endings[endings.length - 1] == ch) pop_end (); 
            s = s.substring (next+1);
            break; 
          case '\x03':
          case '[':
            {
              if (!with_links && endings == null) {
                get_out = true;
                break;
              }
              // Image links must be treated specially, since they may contain nested links
              // in the caption!
              var initial = null;  // If set, it's 'image:' or 'file:' and we have an image link
              i = next;
              while (i < s.length && s.charAt (i) == ch) i++;
              if (ch == '\x03' && i < s.length && s.charAt (i) == '[') i++;
              function get_initial (i, s)
              {
                for (var j = 0; j < TextCleaner.imgNamespaceNames.length; j++) {
                  if (s.length >= i + TextCleaner.imgNamespaceNames[j].length + 1) {
                    var t = s.substr (i, TextCleaner.imgNamespaceNames[j].length + 1);
                    if (t.toLowerCase() == (TextCleaner.imgNamespaceNames[j].toLowerCase () + ':'))
                      return t;
                  }
                }
                return null;
              }
              initial = get_initial (i, s);

              // Scan ahead. We'll break at the next top-level | or ] or ]] or [ or [[ or {| or |}
              var lk_text = sanitize (s.substring (i),
                                      false,           // No links at top-level allowed
                                      caption_level + 1,
                                      false,           // No thumbs
                                      true,            // Break at pipe
                                      false,           // No tables
                                      false);          // No galleries
              var lk_text_length = consumed[caption_level];
              j = i + lk_text_length;
              if (j >= s.length) {
                // Used up the whole text: [[Foo or [bar
                if (initial != null && allow_only_thumbs)
                  // Should in any case have started with [[, not [
                  result = result + s.substring (0, i-1) + '\x03:' + initial
                         + lk_text.substring (initial.length) + '\x04';
                else
                  result = result + s.substring (0, i) + lk_text
                         + ((s.charAt (i-1) == '[') ? ']' : '\x04');
                s = "";
                break;
              }
              if (s.charAt (j) == '|') k = j; else k = -1;
              if (k < 0) {
                // No pipe found: we should be on the closing ]] or ] or [[Foo]] or [bar]
                if (initial != null && allow_only_thumbs)
                  // Should in any case have started with [[, not [
                  result = result + s.substring (0, i-1) + '\x03:' + initial
                         + lk_text.substring (initial.length) + '\x04';
                else
                  result = result + s.substring (0, i) + lk_text
                         + ((s.charAt (i-1) == '[') ? ']' : '\x04');
                if (s.charAt (j) == ']' || s.charAt (j) == '\x04') {
                  // Indeed closing the link
                  s = s.substring (j+1);
                } else {
                  s = s.substring (j);
                }
                break;
              } else {
                var caption = null;
                var used    = 0;
                // Pipe found.
                if (initial == null) {
                  // Not an image link. Must be something like [[Foo|Bar]].
                  caption = sanitize
                              ( s.substring (k+1)
                               , false             // No links, please
                               , caption_level + 1
                               , false             // No thumbs either
                               , false             // Don't care about pipes
                               , true              // Allow tables (yes, parser allows that!)
                               , true);            // Allow galleries (?)
                  // Now we're at [[, [, ]], or ]
                  used = consumed[caption_level];
                  result = result + s.substring (0, i) + lk_text + '|' + caption
                         + ((s.charAt (i-1) == '[') ? ']' : '\x04');
                } else {
                  var q = s.substring (k);
                  // We assume that there are no templates, nowikis, and other nasty things
                  // in the parameters. Search forward until the next [, {, ], }
                  l = q.search(/[\x01\x02\x03[\x04\]\{\}\x05\x06\x07\x08]/);
                  if (l < 0) l = q.length;
                  if (l+1 < q.length) q = q.substring (0, l+1);
                  var is_thumb = q.search (/\|\s*thumb(nail)?\s*[\|\x04]/) >= 0;
                  var img_width = /\|\s*(\d+)px\s*[\|\x04]/.exec (q);
                  if (img_width && img_width.length > 1) {
                    img_width = parseInt (img_width[1], 10);
                    if (isNaN (img_width)) img_width = null;
                  } else
                    img_width = null;
                  if (img_width === null) img_width = is_thumb ? 180 : 301;
                  var is_small = img_width <= 300;
                  
                  // Caption starts at the last pipe before l. If that is a parameter,
                  // it doesn't hurt.
                  var m = k + q.lastIndexOf ('|', l);
                  caption = sanitize
                              (  s.substring (m+1)
                               , is_thumb                  // Allow links only if it's a thumb
                               , caption_level + 1
                               , allow_thumbs && is_thumb
                               , false                     // Don't break at pipe
                               , is_thumb                  // Tables only if it's a thumb
                               , is_thumb);                // Allow galleries for thumbs (?)
                  used = consumed[caption_level];
                  // caption used 'used' chars from m+1, s.charAt (m+1+used) == '\x04'
                  is_thumb = allow_thumbs && is_small;
                  if (is_thumb || !allow_only_thumbs)
                    result = result + s.substring (0, i-1) + '\x03' + lk_text ;
                  else
                    result = result + s.substring (0, i-1) + '\x03:' + initial
                           + lk_text.substring (initial.length);
                  result = result + s.substring (k, m+1) + caption + '\x04';
                  k = m;
                }
                next = k+1+used;
                if (next < s.length) {
                  if (s.charAt (next) != '\x04')
                    s = s.substring (next);
                  else
                    s = s.substring (next+1);
                } else
                  s = "";
              }
              break;
            }
          case '\x04':
          case ']':
            // Extra bracket.
            result = result + s.substring (0, next);
            if (caption_level == 0 && !break_at_pipe) {
              result = result + (ch == ']' ? '&#x5D;' : '&#x5D;&#x5D;');
              s = s.substring (next+1);
            } else
              get_out = true;
            break;
          case '|':
            result = result + s.substring (0, next);
            if (break_at_pipe && endings == null) {
              // Pipe character at top level
              get_out = true;
            } else {
              if (caption_level == 0 && !break_at_pipe && endings == null)
                result = result + '&#x7C;'; // Top-level pipe character
              else
                result = result + '|';
              s = s.substring (next+1);
            }
            break;
        } // end switch
      } // end while
      if (in_nowiki) result = result + "\<\/nowiki>"; // Make sure this nowiki is closed.
      // Close open templates and tables
      while (endings != null) {
        ch = pop_end ();
        result = result + (ch == '\x06' ? '\n' : "") + ch;
      }
      if (caption_level > 0) {
        var used_up = initial_length - (get_out ? (s.length - next) : 0);
        if (consumed.length < caption_level)
          consumed[consumed.length] = used_up;
        else
          consumed[caption_level-1] = used_up;
      }
      return result;      
    }
    
    // Replace multi-character tokens by one-character placeholders, simplifying the
    // subsequent processing.
    var s = input.replace (/\{\{/g, '\x01')
                 .replace (/\n\s*\|\}\}\}/g, '\n\x06\x02') // Table end + template end
                 .replace (/\}\}/g, '\x02')
                 .replace (/\[\[/g, '\x03')
                 .replace (/\]\]/g, '\x04')
                 .replace (/\n\s*\{\|/g, '\n\x05')       // Table start and end must be on own line
                 .replace (/^\s*\{\|/, '\x05')           // Table start at the very beginning
                 .replace (/\n\s*\|\}/g, '\n\x06')       // (we strip leading whitespace)
                 .replace (/\<\s*gallery\s*\>/g, '\x07')
                 .replace (/\<\/\s*gallery\s*\>/g, '\x08');

    s = sanitize (s, true, 0, true, false, true, true);
    // with links, allow thumbs, don't break at pipe, allow tables, allow galleries
    return s.replace (/\x01/g, '\{\{')
            .replace (/\x02/g, '\}\}')
            .replace (/\x03/g, '\[\[')
            .replace (/\x04/g, '\]\]')
            .replace (/\x05/g, '\{\|')
            .replace (/\x06/g, '\|\}')
            .replace (/\x07/g, '<gallery>')
            .replace (/\x08/g, '</gallery>');
  }
}

// </source>