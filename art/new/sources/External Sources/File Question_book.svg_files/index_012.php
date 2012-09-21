// <source lang="javascript">
 
/*
  Site-wide configurations and start of the ImageAnnotator gagdet. Split into a
  separate file for three reasons:
  1. It separates the configuration from the core code, while still
  2. making it impossible for someone else (e.g. a malicious user) to override these
     defaults, and
  3. makes configuration changes available quickly: clients cache this file for four hours.
 
  Author: [[User:Lupo]], September 2009
  License: Quadruple licensed GFDL, GPL, LGPL and Creative Commons Attribution 3.0 (CC-BY-3.0)
 
  Choose whichever license of these you like best :-)
 
  See http://commons.wikimedia.org/wiki/Help:Gadget-ImageAnnotator for documentation.
*/

(function ()
{
  var wgUserGroups = window.wgUserGroups;

  // Global settings. Edit these to configure ImageAnnotator for your Wiki. Note: these configurations
  // are here to prevent them to be overwritten by a user in his or her user scripts. BE EXTRA CAREFUL
  // IF YOU CHANGE THESE SETTINGS WHEN IMAGEANNOTATOR IS ALREADY DEPLOYED! Syntax or other errors here
  // may break ImageAnnotator for everyone!
  var config = {

    // By default, ImageAnnotator is enabled in all namespaces (except "Special", -1) for everyone,
    // except on the project's main page.
    // Here, you can define a list of namespaces where it is additionally disabled.
    viewingEnabled : function ()
    {
      return wgNamespaceNumber >= 0
             && (typeof (wgMainPageTitle) == 'undefined' || !wgMainPageTitle // Available only since MW 1.16
                || wgPageName != wgMainPageTitle.replace (/ /g, '_')) // Disable on the main page
             && (wgAction && (wgAction == 'view' || wgAction == 'purge' || wgAction == 'submit')); 
    },
    // For instance, to disable ImageAnnotator on all talk pages, replace the function body above by
    //     return (wgNamespaceNumber & 1) == 0;
    // Or, to disable it in the category namespace and on article talk pages, you could use
    //     return (wgNamespaceNumber != 14) && (wgNamespaceNumber != 1);
    // To enable viewing only on file description pages and on pages in the project namespace:
    //     return (wgNamespaceNumber == 6) || (wgNamespaceNumber == 4);
    // To enable viewing only for logged-in users, use
    //     return wgUserName !== null;
    // To switch off viewing of notes on the project's main page, use
    //     return wgPageName != wgMainPageTitle.replace (/ /g, '_');

    // By default, editing is enabled for anyone on the file description page or the page that contains
    // the substitution of template ImageWithNotes. Here, you can restrict editing even more, for
    // instance by allowing only autoconfirmed users to edit notes through ImageAnnotator. Note that
    // editing is only allowed if viewing is also allowed.
    editingEnabled : function ()
    {
      var pageView = wgAction && (wgAction == 'view' || wgAction == 'purge')
                     && document.URL.search (/[?&](diff|oldid)=/) < 0;
      if (!pageView) return false;
      if (   (wgNamespaceNumber == 2 || wgNamespaceNumber == 3)
          && wgUserName && wgTitle.replace (/ /g, '_').indexOf (wgUserName.replace (/ /g, '_')) == 0
         ) {
        // Allow anyone to edit notes in their own user space (sandboxes!)
        return true;
      }
      // Otherwise restrict editing of notes to autoconfirmed users.
      return    wgUserGroups
             && (wgUserGroups.join (' ') + ' ').indexOf ('confirmed ') >= 0; // Confirmed and autoconfirmed
      
    },
    // To allow only autoconfirmed users to edit, use
    //     return wgUserGroups &&
    //            (' ' + wgUserGroups.join (' ') + ' ').indexOf (' autoconfirmed ') >= 0;
    // The following example restricts editing on file description pages to autoconfirmed users,
    // and otherwise allows edits only on subpages in the project namespace (for instance, featured
    // image nominations...), but allows editing there for anyone.
    //     return (   (   wgNamespaceNumber == 6
    //                 && wgUserGroups
    //                 && (' ' + wgUserGroups.join (' ') + ' ').indexOf (' autoconfirmed ') >= 0
    //                )
    //             || (wgNamespaceNumber == 4 && wgPageName.indexOf ('/') > 0)
    //            );
    // Note that wgUserName is null for IPs.

    // If editing is allowed at all, may the user remove notes through the ImageAnnotator interface?
    // (Note that notes can be removed anyway using a normal edit to the page.)
    mayDelete : function ()
    {
      return true;
    },

    // If the user may delete notes, may he or she delete with an empty deletion reason?
    emptyDeletionReasonAllowed : function ()
    {
      if (!wgUserGroups) return false;
      var groups = ' ' + wgUserGroups.join (' ') + ' ';
      return groups.indexOf (' sysop ') >= 0 || groups.indexOf (' rollbacker ') >= 0;
    },

    // If the user may delete, may he or she bypass the prompt for a deletion reason by setting
    // var ImageAnnotator_noDeletionPrompt = true;
    // in his or her user scripts?
    mayBypassDeletionPrompt : function ()
    {
      if (!wgUserGroups) return false;
      return (' ' + wgUserGroups.join (' ') + ' ').indexOf (' sysop ') >= 0;
    },

    // If viewing is enabled at all, you can specify here whether viewing notes on thumbnails (e.g.,
    // in articles) is switched on. Logged-in users can augment this by disabling viewing notes on
    // thumbnails on a per-namespace basis using the global variable ImageAnnotator_no_thumbs.
    thumbsEnabled : function ()
    {
      return true;
    },
    // For instance, to switch off viewing of notes on thumbnails for IPs in article space, you'd use
    //     return !(namespaceNumber == 0 && wgUserName === null);

    // If viewing is enabled at all, you can define whether viewing notes on non-thumbnail images is
    // switched on. Logged-in users can augment this by disabling viewing notes on non-thumbnails
    // on a per-namespace basis using the global variable ImageAnnotator_no_images.
    generalImagesEnabled : function ()
    {
      return true;
    },

    // If thumbs or general images are enabled, you can define whether this shall apply only to local
    // images (return false) or also to images that reside at the shared repository (the Commons). In
    // the 'File:' namespace, displaying notes on shared images is always enabled. (Provided viewing
    // notes is enabled at all there. If you've disabled viewing notes in all namespaces including
    // the 'File:' namespace for non-logged-in users, they won't see notes on images from the Commons
    // either, even if you enable it here.)
    sharedImagesEnabled : function ()
    {
      return true;
    },

    // If thumbs or general images are enabled, you can define here whether you want to allow the
    // script to  display the notes or just a little indicator (an icon in the upper left--or right
    // on rtl wikis--corner of the image). The parameters given are
    //   name         string
    //     the name of the image, starting with "File:"
    //   is_local     boolean
    //     true if the image is local, false if it is from the shared repository
    //   thumb        object {width: integer, height: integer}
    //     Size of the displayed image in the article, in pixels
    //   full_img     object {width: integer, height: integer}
    //     Size of the full image as uploaded, in pixels
    //   nof_notes    integer 
    //     Number of notes on the image
    //   is_thumbnail boolean
    //     true if the image is a thumbnail, false otherwise
    inlineImageUsesIndicator : function (name, is_local, thumb, full_img, nof_notes, is_thumbnail)
    {
      // Of course you could also use wgNamespace or any other of the wg-globals here.
      return    (is_thumbnail && !is_local)
             || ((   thumb.width < 250 && thumb.height < 250
                  && (thumb.width < full_img.width || thumb.height < full_img.height)
                 )
                   ? nof_notes > 10 : false
                );
      // This default displays only an indicator icon for non-local thumbnails,
      // and for small images that are scaled down, but have many notes
    },

    // If notes are displayed on an image included in an article, ImageAnnotator normally adds a
    // caption indicating the presence of notes. If you want to suppress this for all images included
    // in articles, return false. To suppress the caption only for thumbnails, but not for otherwise
    // included images, return !is_thumbnail. To suppress the caption for all images but thumbnails,
    // return is_thumbnail. The parameters are the same as for the function inlineImageUsesIndicator
    // above.
    displayCaptionInArticles : function (name, is_local, thumb, full_img, nof_notes, is_thumbnail)
    {
      return true;
    },

    // Different wikis may have different image setups. For the Wikimedia projects, the image
    // servers are set up to generate missing thumbnails on the fly, so we can just construct
    // a valid thumbnail url to get a thumbnail, even if there isn't one of that size yet.
    // Return true if your wiki has a similar setup. Otherwise, return false.
    thumbnailsGeneratedAutomatically : function ()
    {
      return true;
    },

    // Determine whether an image is locally stored or comes from a central repository. For wikis
    // using the Commons as their central repository, this should not need changing.
    imageIsFromSharedRepository : function (img_url)
    {
      return wgServer.indexOf ('/commons') < 0 && img_url.indexOf ('/commons') >= 0;
    },

    // Return the URL of the API at the shared file repository. Again, for wikis using the Commons
    // as their central repository, this should not need changing. If your wiki is accessible through
    // https, it's a good idea to also make the shared repository accessible through https and return
    // that secure URL here to avoid warnings about accessing a non-secure site from a secure site.
    sharedRepositoryAPI : function ()
    {
      return '//commons.wikimedia.org/w/api.php';
    },

    // Default coloring. Each note's rectangle has an outer and an inner border.
    outer_border  : '#666666', // Gray
    inner_border  : 'yellow',
    active_border : '#FFA500', // Orange, for highlighting the rectangle of the active note
    new_border    : 'red',     // For drawing rectangles

    // Default threshold for activating the zoom (can be overridden by users).
    zoom_threshold : 8.0,

    UI : {
      defaultLanguage : wgContentLanguage, // Don't change this!

      // Translate the texts below into the wgContentLanguage of your wiki. These are used as
      // fallbacks if the localized UI cannot be loaded from the server.
      defaults: {
         wpImageAnnotatorDelete        : 'Delete'
        ,wpImageAnnotatorEdit          : 'Edit'
        ,wpImageAnnotatorSave          : 'Save'
        ,wpImageAnnotatorCancel        : 'Cancel'
        ,wpImageAnnotatorPreview       : 'Preview'
        ,wpImageAnnotatorRevert        : 'Revert'
        ,wpTranslate                   : 'translate'
        ,wpImageAnnotatorAddButtonText : 'Add a note'
        ,wpImageAnnotatorAddSummary    :
          '[[MediaWiki talk:Gadget-ImageAnnotator.js|Adding image note]]$1'
        ,wpImageAnnotatorChangeSummary :
          '[[MediaWiki talk:Gadget-ImageAnnotator.js|Changing image note]]$1'
        ,wpImageAnnotatorRemoveSummary :
          '[[MediaWiki talk:Gadget-ImageAnnotator.js|Removing image note]]$1'
        ,wpImageAnnotatorHasNotesShort : 'This file has annotations.'
        ,wpImageAnnotatorHasNotesMsg   :
           'This file has annotations. Move the mouse pointer over the image to see them.'
        ,wpImageAnnotatorEditNotesMsg  :
           '<span>\xa0To edit the notes, visit page <a href="#">x</a>.</span>'
        ,wpImageAnnotatorDrawRectMsg   :
           'Draw a rectangle onto the image above (mouse click, then drag and release)'
        ,wpImageAnnotatorEditorLabel   :
           '<span>Text of the note (may include '
         + '<a href="//meta.wikimedia.org/wiki/Help:Reference_card">Wiki markup</a>)</span>'
        ,wpImageAnnotatorSaveError  :
           '<span><span style="color:red;">'
         + 'Could not save your note (edit conflict or other problem).'
         + '</span> '
         + 'Please copy the text in the edit box below and insert it manually by '
         + '<a href="'
         + wgArticlePath.replace ('$1', encodeURI (wgPageName))
         + '?action=edit">editing this page</a>.</span>'
        ,wpImageAnnotatorCopyright :
           '<small>The note will be published multi-licensed as '
         + '<a href="http://creativecommons.org/licenses/by-sa/3.0/">CC-BY-SA-3.0</a> and '
         + '<a href="http://www.gnu.org/copyleft/fdl.html">GFDL</a>, versions 1.2 and 1.3. '
         + 'Please read our <a href="//wikimediafoundation.org/wiki/Terms_of_Use">terms '
         + 'of use</a> for more details.</small>'
        ,wpImageAnnotatorDeleteReason :
           'Why do you want to remove this note?'
        ,wpImageAnnotatorDeleteConfirm :
           'Do you really want to delete this note?'
        ,wpImageAnnotatorHelp          : 
           '<span><a href="//commons.wikimedia.org/wiki/Help:Gadget-ImageAnnotator" '
         + 'title="Help">Help</a></span>'
        // The following image should be a GIF or an 8bit indexed PNG with transparent background,
        // to make sure that even IE6 displays the transparency correctly. A normal 32bit PNG might
        // display a transparent background as white on IE6.
        ,wpImageAnnotatorIndicatorIcon :
           '<span>'
         + '<img src="//upload.wikimedia.org/wikipedia/commons/8/8a/Gtk-dialog-info-14px.png" '
         + 'width="14" height="14" title="This file has annotations" />'
         + '</span>'
        ,wpImageAnnotatorCannotEditMsg :
           '<span>To modify annotations, your browser needs to have the '
         + '<a href="//en.wikipedia.org/wiki/XMLHttpRequest">XMLHttpRequest</a> '
         + 'object. Your browser does not have this object or does not allow it to be used '
         + '(in Internet Explorer, it may be in a switched off ActiveX component), and '
         + 'thus you cannot modify annotations. We\'re sorry for the inconvenience.</span>'
      }
    }

  }; // End site-wide config.

  // DO NOT CHANGE ANYTHING BELOW THIS LINE
 
  // Start of ImageAnnotator
  if (config.viewingEnabled ()) {
    addOnloadHook (function () {ImageAnnotator.install (config);});
  }
  
})();