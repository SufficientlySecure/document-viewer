// [[File:Erioll_world.svg|18px]] '''WikiMiniAtlas''' <br>
// Script to embed interactive maps into pages that have coordinate templates <br>
// also check my user page [[User:Dschwen]] for more toolsz
// <nowiki>
//
// Revision 16.5

jQuery(function ($) {
 var config = {
  width  : 600,
  height : 400,
  timeout : 5000,
  zoom : -1,
  quicklink : false,
  quicklinkurl : '//maps.google.com/maps?ll={latdegdec},{londegdec}&spn={span},{span}&q={latdegdec},{londegdec}',
  enabled : true,
  onlytitle : false,
  flowTextTooltips: (location.host === "en.wikipedia.org"),
  alwaysTooltips: false,
  iframeurl : '//toolserver.org/~dschwen/wma/iframe2.html',
  imgbase   : '//toolserver.org/~dschwen/wma/tiles/',
  buttonImage: '//upload.wikimedia.org/wikipedia/commons/thumb/5/55/WMA_button2b.png/17px-WMA_button2b.png'
 },
 strings = {
  buttonTooltip : {
   af:'Vertoon ligging op \'n interaktiwe kaart.',
   als:'Ort uf dr interaktivä Chartä zeigä',
   ar:'شاهد الموقع على الخريطة التفاعلية',
   'be-tarask':'паказаць месцазнаходжаньне на інтэрактыўнай мапе',
   'be-x-old':'паказаць месцазнаходжаньне на інтэрактыўнай мапе',
   bar:'Ort af da interaktivn Kartn zoagn',
   bg:'покажи местоположението на интерактивната карта',
   bpy:'জীবন্ত মানচিত্রগর মা মাপাহান দেখাদিতই',
   br:'diskouez al lec\'hiadur war ur gartenn etrewezhiat',
   ca:'mostra la localització en un mapa interactiu',
   cs:'zobraz místo na interaktivní mapě',
   da:'vis beliggenhed på interaktivt kort',
   de:'Ort auf interaktiver Karte anzeigen',
   dsb:'Městno na interaktiwnej kórśe zwobrazniś',
   fa:'نمایش مکان در نقشه‌ای پویا',
   el:'εμφάνιση τοποθεσίας σε διαδραστικό χάρτη',
   en:'Show location on an interactive map',
   bn:'সক্রিয় মানচিত্রে অবস্থান চিহ্নিত করুন',
   eo:'Montru lokigon sur interaktiva karto',
   eu:'erakutsi kokalekua mapa interaktibo batean',
   es:'mostrar el lugar en un mapa interactivo',
   fr:'Montrer la localisation sur une carte interactive',
   fur:'mostre la localizazion suntune mape interative',
   fy:'it plak op in oanpasbere kaart oanjaan',
   gl:'Amosar o lugar nun mapa interactivo',
   he:'הראה מיקום במפה האינטראקטיבית',
   hi:'सक्रिय नक्शे पर लोकेशन या स्थान दिखायें', 
   hr:'prikaži lokaciju na interaktivnom zemljovidu',
   hsb:'Městno na interaktiwnej karće zwobraznić',
   hu:'Mutasd a helyet egy interaktív térképen!',
   hy:'ցուցադրել դիրքը ինտերակտիվ քարտեզի վրա',
   id:'Tunjukkan lokasi di peta interaktif',
   ilo:'Ipakita ti lokasion idiay interaktibo a mapa',
   is:'sýna staðsetningu á gagnvirku korti',
   it:'mostra la località su una carta interattiva',
   ja:'インタラクティブ地図上に位置を表示',
   km:'បង្ហាញទីតាំងនៅលើផែនទីអន្តរកម្ម',
   ko:'인터랙티브 지도에 위치를 표시',
   lt:'Rodyti vietą interaktyviame žemėlapyje',
   lv:'Rādīt atrašanās vietu interaktīvajā kartē',
   mk:'прикажи положба на интерактивна карта',
   nl:'de locatie op een interactieve kaart tonen',
   no:'vis beliggenhet på interaktivt kart',
   nv:'kéyah tʼáá dah siʼą́ą́ ńtʼę́ę́ʼ beʼelyaaígíí',
   pl:'Pokaż lokalizację na mapie interaktywnej',
   pt:'mostrar a localidade num mapa interactivo',
   ro:'Arată locaţia pe o hartă interactivă',
   ru:'показать положение на интерактивной карте',
   sk:'zobraz miesto na interaktívnej mape',
   sl:'Prikaži lego na interaktivnem zemljevidu',
   sq:'trego vendndodhjen në hartë',
   fi:'näytä paikka interaktiivisella kartalla',
   sv:'visa platsen på en interaktiv karta',
   tr:'Yeri interaktif bir haritada göster',
   uk:'показати положення на інтерактивній карті',
   vi:'xem vị trí này trên bản đồ tương tác',
   vo:'Jonön topi su kaed itjäfidik',
   zh:'显示该地在地图上的位置',
   'zh-cn':'显示该地在地图上的位置',
   'zh-sg':'显示该地在地图上的位置',
   'zh-tw':'顯示該地在地圖上的位置',
   'zh-hk':'顯示該地在地圖上的位置'
  },
  close : {
   af:'Sluit',
   als:'Zuä machä',
   ar:'غلق',
   'be-tarask':'закрыць',
   'be-x-old':'закрыць',
   bar:'zuamachn',
   bg:'затвори',
   bpy:'জিপা',
   br:'serriñ',
   ca:'tanca',
   cs:'zavřít',
   da:'luk',
   de:'schließen',
   dsb:'zacyniś',
   nv:'doo yishʼį́ nisin da',
   el:'έξοδος',
   en:'close',
   bn:'বন্ধ করুন',
   eo:'fermu', 
   eu:'itxi',
   es:'cerrar',
   fa:'بستن',
   fr:'Quitter',
   fur:'siere',
   fy:'ticht',
   gl:'pechar',
   he:'לסגור',
   hi:'बंद करें',
   hr:'zatvori',
   hsb:'začinić',
   hu:'bezárás', 
   hy:'փակել',
   id:'tutup',
   ilo:'irikep',
   is:'loka',
   it:'chiudi',
   ja:'閉じる',
   km:'បិទ',
   ko:'닫기',
   lt:'uždaryti',
   lv:'aizvērt',
   mk:'затвори',
   nl:'sluiten',
   no:'lukk',
   pl:'zamknij',
   pt:'fechar',
   ro:'închide',
   ru:'закрыть',
   sk:'zatvoriť',
   sl:'zapri',
   sq:'mbylle',
   fi:'sulje',
   sv:'stäng',
   tr:'kapat',
   uk:'закрити',
   vi:'đóng',
   vo:'färmükön',
   zh:'关闭',
   'zh-cn':'关闭',
   'zh-sg':'关闭',
   'zh-tw':'關閉',
   'zh-hk':'關閉'
  },
  resize : {
   ar: 'تغيير حجم',
   ca: 'redimensionar',
   de: 'Größe ändern',
   dk: 'ændre størrelse',
   en: 'resize',
   es: 'cambiar el tamaño',
   fi: 'muuta kokoa',
   fr: 'redimensionner',
   ja: 'サイズを変更する',
   nl: 'vergroten of verkleinen',
   no: 'endre størrelse',
   sv: 'ändra storlek'
  }
 },

 language = '', site = '', awt="0",
 iframe = { div: null, iframe: null, closebutton: null, resizebutton: null, resizehelper: null, indom: false },
 
 page_title = (mw.config.get( 'wgNamespaceNumber' )==0) ? encodeURIComponent(mw.config.get( 'wgTitle' )) : '',

 bodyc,
 coord_filter = /&params=([\d.+-]+)_([\d.+-]*)_?([\d.+-]*)_?([NSZ])_([\d.+-]+)_([\d.+-]*)_?([\d.+-]*)_?([EOW])([^&=<>|]{0,250})/,
 coord_list = [],
 coord_highlight = -1,
 //region_index = 0,
 //coordinate_region = '',

 kml = null,
 mes = null,

 quicklinkbox = null,
 quicklinkdest = null;

 // check if we are in a right-to-left-script project
 function isRTL() {
  return /(^|\s)rtl(\s|$)/.test(document.body.className);
 }

 // get position on page
 function yPos(el) {
  return $(el).offset().top + $(el).outerHeight();
 }

 // show, move, and update iframe
 function showIFrame(e) {
  var wi = iframe, my = yPos(this),
      newurl = config.iframeurl + '?wma=' + e.data.param + '&lang=' + site + '&page=' + page_title + '&awt=' + awt;

  // insert iframe into DOM on demand (to preserve page caching)
  if( !wi.indom ) {
   $('#content,#mw_content').prepend(wi.div);
   wi.indom = true;
  }

  if( wi.iframe.attr('src') !== newurl ) {
   wi.iframe.attr( 'src', newurl );
  } else if( wi.div.css('display') !== 'none' ) {
   wi.div.hide();
   return false;
  }
  wi.div.css( 'top', my+'px' ).show();
  return false;
 }

 // fill in the map-service templates 
 function qlURL( lat, lon, zoom ) {
  var url  = config.quicklinkurl,
      span = Math.pow( 2.0, zoom) / 150.0;

  url = url.replace( /\{latdegdec\}/g, lat );
  url = url.replace( /\{londegdec\}/g, lon );
  url = url.replace( /\{span\}/g, span.toFixed(4) );

  return url;
 }

 function highlight(i) {
  if( coord_highlight >= 0 ) {
   $(coord_list[coord_highlight].obj).css('background-color','').find('span:visible').css('background-color','');
  }
  coord_highlight = i;
  if( coord_highlight >= 0 ) {
   $(coord_list[coord_highlight].obj).css('background-color','yellow').find('span:visible').css('background-color','yellow');
  }
 }

 function messageHub(e) {
  var i, d, clist = { coords: [] }
    , geoext = [], sx=0, sy=0, s
    , minlat = Infinity, maxlat = -Infinity, ineg = -1, ipos = -1;
  e = e.originalEvent;
  d = e.data.split(',');
  mes = e.source;
  switch(d[0]) { 
   case 'request' :
    // make a JSON encodable copy of coord_list (no HTML objects!)
    // find center and extent
    for( i = 0; i < coord_list.length; ++i ) {
     clist.coords[i] = {
      lat: coord_list[i].lat,
      lon: coord_list[i].lon,
      title: coord_list[i].title.replace(/[\+_]/g,' ')
     };
     if(  coord_list[i].lat < minlat ) { minlat = coord_list[i].lat; }
     if(  coord_list[i].lat > maxlat ) { maxlat = coord_list[i].lat; }
     geoext[i] = {
      x: Math.cos(coord_list[i].lon/180.0*Math.PI),
      y: Math.sin(coord_list[i].lon/180.0*Math.PI)
     }
     sx += geoext[i].x;
     sy += geoext[i].y;
    }
    clist.loncenter = Math.atan2(sy,sx)*180.0/Math.PI;
    clist.latmax = maxlat;
    clist.latmin = minlat;
    // extent in longitude
    for( i = 0; i < geoext.length; ++i ) {
     s = (geoext[i].x*sy-geoext[i].y*sx);
     geoext[i].z = (geoext[i].x*sx+geoext[i].y*sy);
     if( s<0 && ( ineg<0 || geoext[i].z<geoext[ineg].z ) ) { ineg=i; }
     if( s>0 && ( ipos<0 || geoext[i].z<geoext[ipos].z ) ) { ipos=i; }
    }
    if( ipos>=0 && ineg>=0 ) {
     clist.lonleft  = coord_list[ipos].lon;
     clist.lonright = coord_list[ineg].lon;
    }
    if( typeof JSON !== "undefined" ) {
     mes.postMessage( JSON.stringify(clist), document.location.protocol + '//toolserver.org' );
     if( kml !== null ) {
      mes.postMessage( JSON.stringify(kml), document.location.protocol + '//toolserver.org' );
     }
    }
   case 'unhighlight' :
    highlight(-1);   
    break;
   case 'toggle' : 
    coord_list[parseInt(d[1])].mb.click();
    break;
   case 'scroll' :
    $("html:not(:animated),body:not(:animated)").animate({ scrollTop: $(coord_list[parseInt(d[1])].obj).offset().top - 20 + parseInt(d[2]||0) }, 500 );
    iframe.div.css( { top: yPos( coord_list[parseInt(d[1])].obj ) + 'px'} );
    // make sure scroll target gets highlighted
    setTimeout( function () { highlight(parseInt(d[1])); }, 200 );
    break;
   case 'highlight' :
    highlight(parseInt(d[1]));
    break;
  }  
 }

 // parse url parameters into a hash
 function parseParams(url) {
  var map = {}, h, i, pair = url.substr(url.indexOf('?')+1).split('&');
  for( i=0; i<pair.length; ++i ) {
   h = pair[i].split('=');
   map[h[0]] = h[1];
  }
  return map;
 }

 // Insert the IFrame into the page.

 var wi = iframe,
     wc = config,
     marker = { lat:0, lon:0 }, coordinates = null,
     link, links, key, len, coord_title, icons, startTime, content, mapbutton;

 // apply settings
 if( typeof(wma_settings) === 'object' ) {
  for( key in wma_settings ) {
   if( typeof(wma_settings[key]) === typeof(wc[key]) ) {
    wc[key] = wma_settings[key];
   }
  }
 }

 if( wc.enabled === false ) { return; }

 site = ( wgNoticeProject == "commons" ) ? "commons" : wgPageContentLanguage;
 language = mw.config.get( 'wgUserLanguage' );

 // remove icons from title coordinates
 $('#coordinates,#coordinates-title,#tpl_Coordinaten').find('a.image').detach();
 
 bodyc = $( wc.onlytitle ? '#coordinates,#coordinates-title' : 'html' );
 startTime = (new Date()).getTime();

 bodyc.find('a.external.text').each( function( key, link ) {
  var ws, coord_params, params, zoomlevel, globe="Earth";

  // check for timeout (every 10 links only)
  if( key % 10 === 9 && (new Date()).getTime() > startTime + wc.timeout ) { 
   return false; // break out of each
  }

  if( !link.href || !coord_filter.exec(link.href) ){ // invalid links do not contain href attribute in IE!
   return true;
  }
  marker.lat=(1.0*RegExp.$1) + ((RegExp.$2||0)/60.0) + ((RegExp.$3||0)/3600.0);
  if( RegExp.$4 !== 'N' ) { marker.lat*=-1; }
  marker.lon=(1.0*RegExp.$5) + ((RegExp.$6||0)/60.0) + ((RegExp.$7||0)/3600.0);
  if( RegExp.$8 === 'W' ) { marker.lon*=-1; }
  coord_params = RegExp.$9;

  // Zoom based on coordinate N/S precision 
  var coord_digits = RegExp.$3 ? 4 : RegExp.$2 ? 2 : RegExp.$1.length - (RegExp.$1+".").indexOf('.') - 1;
  zoomlevel = coord_digits * Math.log(10)/Math.log(2);

  // Find a sensible Zoom-level based on type
  if( /_type:(airport|edu|pass|landmark|railwaystation)/.test(coord_params) ) {
   zoomlevel = 8;
  } else if( /_type:(event|forest|glacier)/.test(coord_params) ) {
   zoomlevel = 6;
  } else if( /_type:(adm3rd|city|mountain|isle|river|waterbody)/.test(coord_params) ) {
   zoomlevel = 4;
  }

  // wma shows dim approx 4e7m at zoom 0 or 1.5e8 is the scale of zoomlevel 0
  if( /_dim:([\d.+-]+)(km|m|_|$)/.exec(coord_params) ) {
   zoomlevel = Math.log( ( RegExp.$2 === "km" ? 4e4 : 4e7 ) / RegExp.$1 ) / Math.log(2);
  }
  if( /_scale:(\d+)(_|$)/.exec(coord_params) ) {
   zoomlevel = Math.log( 1.5e8/RegExp.$1 ) / Math.log(2);
  }

  if( wc.zoom !== -1 ) { zoomlevel = wc.zoom; }
  //if( zoomlevel > 12 ) { zoomlevel = 12; }
  if( zoomlevel < 0 ) { zoomlevel = 0; }

  function capitalize(s) { return s.substr(0,1).toUpperCase()+s.substr(1).toLowerCase(); }
  if( /_globe:([^_&]+)/.test(coord_params) ) { globe = capitalize(RegExp.$1); }
  if( globe!="Earth" && globe!="Moon" && globe!="Mars" && globe!="Venus" && globe!="Mercury" && globe!="Io"  && globe!="Titan" ) { return; }

  // Test the unicode Symbol
  if( site === 'de' && link.parentNode.id !== 'coordinates' ) {
   mapbutton = $('<span>♁</span>').css('color','blue');
  } else {
   mapbutton = $('<img>').attr('src', wc.buttonImage);
  }
  mapbutton.attr( {
   title: strings.buttonTooltip[language] || strings.buttonTooltip.en,
   alt: '' 
  } )
//  .css('opacity', 0.75)
//  .hover(function (){ $(this).fadeTo(50, 1.0); }, function () { $(this).fadeTo(250, 0.75); })
  .hover(function (){ $(this).css('opacity', 0.75); }, function () { $(this).css('opacity', ''); })
  .addClass('noprint')
  .css('padding', isRTL() ? '0px 0px 0px 3px' : '0px 3px 0px 0px' ).css('cursor', 'pointer');

  if( wc.alwaysTooltips || ( wc.flowTextTooltips && $(link).parents('li, table, #coordinates').length == 0 ) ) {
   // insert tooltip rather than icon to improve text readability
   mapbutton = $('<span>').append(mapbutton).append("&nbsp;WikiMiniAtlas").css('cursor','pointer');
   var tooltip = $('<div>').css( {
    backgroundColor: 'white', padding: '0.2em', border: '1px solid black',
    position: 'absolute', top: '1em', left: '0em', 
    display: 'none', zIndex : 15
   }).append(mapbutton);
   $(link).wrap( 
    $('<span/>')
     .css( { position: 'relative', whiteSpace: 'nowrap' } )
     .mouseleave( function () { tooltip.fadeOut() } ) 
    )
    .before( tooltip )
    .mouseenter( function () { tooltip.fadeIn() } );
  } else {
   // insert icon directly
   ws = $(link).css('whiteSpace');
   $(link).wrap( $('<span/>').css('whiteSpace', 'nowrap') ).css('whiteSpace', ws).before(mapbutton);
  }

  mapbutton.bind( 'click', { param:
   marker.lat + '_' + marker.lon + '_' +
   wc.width + '_' + wc.height + '_' +
   site + '_' + zoomlevel + '_' + language + '&globe=' + globe }, showIFrame );

  // store coordinates
  coordinates = link.href;
  params = parseParams(link.href);
  coord_list.push( { lat: marker.lat, lon: marker.lon, obj: link, mb: mapbutton, title: params.title || params.pagename || '' } );

  if ( wc.quicklink ) {
   link.href = qlURL( marker.lat, marker.lon, zoomlevel );
  }
 } ); //end each

 // awareness tooltip 
 // (experiment, only shown to anonymous users on en.wp if the browser supports localStorage)
 if( wgUserName === null && wgDBname === 'enwiki' && 
     window.localStorage ) {
 
  var ttc = window.localStorage.getItem('wmatooltipdismissed') || 0;
  if( ttc<5 && ttc!=='yes') {
   (function() {
    // increase display counter
    window.localStorage.setItem('wmatooltipdismissed',parseInt(ttc)+1);
    var wrap, span=$('#coordinates>span>span>img')
      , bubble = $('<div><div>').css( { width: '150px', border: '1px solid #bbb', borderRadius: '8px', position: 'absolute', bottom: '22px', right: '-4px', whiteSpace: 'normal', textAlign: 'left', padding: '0.25em 0.75em 0.25em 0.75em', fontSize: '90%', backgroundColor: '#f9f9f9', boxShadow: '4px 4px 10px rgba(0,0,0,0.2)' } ).text('Click the blue globe to open an interactive map.').append( $('<div></div>').css({position: 'absolute', width: 0, height: 0, bottom: '-10px', right: '10px', borderBottom:0, borderLeft: '5px solid transparent', borderRight:  '5px solid transparent', borderTop:  '10px solid #bbb'})  ).append( $('<div></div>').css({position: 'absolute', width: 0, height: 0, bottom: '-7px', right: '10px', borderBottom:0, borderLeft: '5px solid transparent', borderRight:  '5px solid transparent', borderTop:  '10px solid #f9f9f9'})  ).hide();
   if( span.length>0 ) {
     wrap = $('<span></span>').css( { width: '20px', height: '17px', position: 'relative' } );
     span.wrap(wrap).parent().append(bubble.fadeIn().click(function(){ bubble.fadeOut(); window.localStorage.setItem('wmatooltipdismissed','yes'); awt='c'; }));
     setTimeout( function() { bubble.fadeOut() }, 10*1000 );
     awt="1";
    }
   })();
  } else { awt=(ttc=='yes')?'p':ttc; }
 }
 // detect and load KML
 // also insert globe even if no title coords are given
 (function () {
  var i, l = $('table.metadata').find('a')
     ,alat = 0, alon = 0, np = 0
     ,la1 = Infinity, la2 =- Infinity
     ,lo1 = Infinity, lo2 =- Infinity
     ,ex,ey;
  for( i = 0; i < l.length; ++i ) {// TODO: replace with .each
   if( $(l[i]).attr('href') === '//en.wikipedia.org/w/index.php?title=Template:Attached_KML/'+mw.config.get( 'wgPageName' )+'&action=raw' ) {
    coordinates = 1;
    $.ajax({
     url: l[i].href,
     dataType: 'xml',
     success: function (xml) {
      function processCoords(t) {
       var way = [], c, p = t.split(' '), i, lat, lon;
       for( i=0; i<p.length; ++i ) {
        c=p[i].split(',');
        if( c.length >= 2 ) {
         lat = parseFloat(c[1]);
         lon = parseFloat(c[0]);
         way.push( { lat: lat, lon: lon } );

         // determine extent of way
         if( lat<la1 ) { la1=lat; }
         if( lon<lo1 ) { lo1=lon; }
         if( lat>la2 ) { la2=lat; }
         if( lon>lo2 ) { lo2=lon; }
        }
       }
       return way;
      }

      // initialize transfer datastructure
      kml = { ways: [], areas: [] };
window.kmldata = xml; // DEBUG!

      // ways
      $(xml).find('LineString > coordinates').each(function () {
       var way = processCoords( $(this).text() );
       if( way.length > 0 ) { kml.ways.push(way); }
      });

      // areas
      $(xml).find('Polygon').each(function () {
       var i, j, c,
        area = { inner: [], outer: [] };

       // outer boundary
       $(this).find('outerBoundaryIs > LinearRing > coordinates').each(function () {
        var way = processCoords( $(this).text() );
        if( way.length > 0 ) {
         area.outer.push(way);
        }
       });

       // inner boundary (holes in the polygon)
       $(this).find('innerBoundaryIs > LinearRing > coordinates').each(function () {
        var way = processCoords( $(this).text() );
        if( way.length > 0 ) { area.inner.push(way); }
       });

       // only add if we have an outer boundary
       if( area.outer.length>0 ) { kml.areas.push(area); }
      });

      // inset min/max extent
      kml.minlon = lo1;
      kml.maxlon = lo2;
      kml.minlat = la1;
      kml.maxlat = la2;
window.kml = kml; // DEBUG!

      // already got a request message
      if( mes !== null && kml.ways.length > 0 && typeof JSON !== "undefined" ) {
       mes.postMessage( JSON.stringify(kml), document.location.protocol + '//toolserver.org' );
      }

      // insert blue globe
      if( coord_list.length == 0 ) {
       // determine center
       alat = (la1+la2)/2;
       alon = (lo1+lo2)/2;

       //determine zoomfactor
       ex = (lo2-lo1)/180.0 * 3.0*128;
       ey = (la2-la1)/180.0 * 3.0*128; // max extent in degrees, zoom0 has 3*128/180 px/degree
       for( zoomlevel = 0; zoomlevel < 12; ++zoomlevel ) {
        if( ex>config.width/2 || ey>config.height/2 ) break;
        ex *= 2; ey *= 2;
       }

       // add mapbutton
       mapbutton = $('<img>')
        .attr('src', wc.buttonImage)
        .bind( 'click', { param:
          alat + '_' + alon + '_' +
          wc.width + '_' + wc.height + '_' +
          site + '_' + zoomlevel + '_' + language 
         }, showIFrame ); // zoomlevel!

       if( $('#coordinates').length ) {
        $('#coordinates').append(mapbutton);
       } else {
        $('<span id="coordinates">Map </span>').append(mapbutton).appendTo('#bodyContent');
       }
      }
     }
    });
    break;
   }
  }
 })();

 // prepare quicklink menu box
 if ( coordinates !== null && wc.quicklink ) {
  quicklinkbox = document.createElement('div');
  // more to come :-)
 }

 // prepare iframe to house the map
 if ( coordinates !== null ) {
  wi.div = $('<div/>').css( {
   width: (wc.width+2)+'px', height: (wc.height+2)+'px',
   margin: '0px', padding: '0px', 
   backgroundColor : 'white', border: '1px solid gray',
   position: 'absolute', top: '1em', zIndex: 13, boxShadow: '3px 3px 25px rgba(0,0,0,0.3)'
  } ).css( isRTL() ? 'left' : 'right', '2em' ).hide();

  var rbrtl = [ '//upload.wikimedia.org/wikipedia/commons/b/b5/Button_resize.png',
                '//upload.wikimedia.org/wikipedia/commons/3/30/Button_resize_rtl.png' ]
  wi.resizebutton = $('<img>').attr( { 
   title : strings.resize[language] || strings.resize.en,
   src : rbrtl[isRTL()?1:0]
  } ).hide().attr('ondragstart','return false');
  
  // cover the iframe to prevent loosing the mouse to the iframe during resizing
  wi.resizehelper = $('<div/>').css( { position: 'absolute', top:0, left:0, zIndex: 20 } ).hide();

  wi.closebutton = $('<img>').attr( { 
   title : strings.close[language] || strings.close.en,
   src : '//upload.wikimedia.org/wikipedia/commons/d/d4/Button_hide.png'
  } ).css( {
   zIndex : 15, position : 'absolute', right : '11px', top : '9px', width : '18px', cursor : 'pointer'
  } ).click( function(e) { wi.div.hide() } );

  wi.iframe = $('<iframe/>').attr( {
   scrolling: 'no',
   frameBorder : 0
  } ).css( {
   zIndex: 14, position: 'absolute', right: '1px', top: '1px',
   width: (wc.width)+'px', height: (wc.height)+'px',
   margin: '0px', padding: '0px'
  } );

  wi.div.append(wi.iframe);
  wi.div.append(wi.resizehelper);
  wi.div.append(wi.closebutton);
  (function () {
   var startx, starty, idle = true, dir = isRTL()?-1:1;
   function adjusthelper() {
    wi.resizehelper.css( { width: (wc.width+2)+'px', height: (wc.height+2)+'px' } );
   }
   wi.div.append(  
    $('<div/>')
     .css( {
       zIndex : 15, position : 'absolute', bottom : '3px', 
       width : '18px', height: '18px', cursor : (isRTL()?'se-resize':'sw-resize'),
       'user-select': 'none', '-moz-user-select': 'none', '-ms-user-select': 'none'
      } ).css( (isRTL()?'right':'left'), '3px' )
     .mouseenter( function(e) { wi.resizebutton.fadeIn() } )
     .mouseleave( function(e) { if( idle ) { wi.resizebutton.fadeOut(); } } )
     .mousedown( function(e) {
       if( idle ) { 
        wi.resizehelper.show();
        adjusthelper();
        lastx = e.pageX;
        lasty = e.pageY;
        $('body').bind('mouseup.wmaresize', function(e) { 
         $('body').unbind('mousemove.wmaresize');  
         $('body').unbind('mouseup.wmaresize'); 
         idle = true;
         wi.resizehelper.hide();
        } );
        $('body').bind('mousemove.wmaresize', function(e) { 
         wc.width -= dir*(e.pageX-lastx);
         wc.height += (e.pageY-lasty);
         lastx = e.pageX; lasty = e.pageY;
         wi.div.css( { width: (wc.width+2)+'px', height: (wc.height+2)+'px' } );
         wi.iframe.css( { width: wc.width+'px', height: wc.height+'px' } );
         adjusthelper();
        } );
        idle = false;
       }
      } )
     .append(wi.resizebutton) 
   );
  })();

  $(window).bind('message', messageHub);
 }
});

// </nowiki>