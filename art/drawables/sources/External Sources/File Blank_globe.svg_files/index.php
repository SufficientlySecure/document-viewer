/**
 * SVG images: adds links to rendered PNG images in different resolutions
 * @author: [[w:en:User:TheDJ]]
 * @source: [[w:en:MediaWiki:Common.js/file.js]]
 */
function SVGThumbs() {
	var	file = document.getElementById( 'file' ), // might fail if MediaWiki can't render the SVG
		i18n = {
			'en': 'This image rendered as PNG in other sizes: ',
			'pt': 'Esta imagem pode ser renderizada em PNG em outros tamanhos: '
		};
	if ( file && mw.config.get( 'wgIsArticle' ) && mw.config.get( 'wgTitle' ).match( /\.svg$/i ) ) {

		// Define language fallbacks
		i18n['en-gb'] = i18n.en;
		i18n['pt-br'] = i18n.pt;
		//i18n['zh-hant'] = i18n.zh;

		// Define interface message
		mw.messages.set( {
			'svg-thumbs-desc': i18n[ mw.config.get( 'wgUserLanguage' ) ] || i18n.en
		} );

		var thumbu = file.getElementsByTagName( 'IMG' )[0].getAttribute( 'src' );
		if( !thumbu ) {
			return;
		}
		var svgAltSize = function( w, title ) {
			var path = thumbu.replace( /\/\d+(px-[^\/]+$)/, '/' + w + '$1' );
			var a = document.createElement( 'A' );
			a.setAttribute( 'href', path );
			a.appendChild( document.createTextNode( title ) );
			return a;
		};

		var p = document.createElement( 'p' );
		p.className = 'SVGThumbs';
		p.appendChild( document.createTextNode( mw.msg( 'svg-thumbs-desc' ) ) );
		var l = [ 200, 500, 1000, 2000 ];
                for( var i = 0; i < l.length; i++ ) {
			p.appendChild( svgAltSize( l[i], l[i] + 'px' ) );
			if( i < l.length-1 ) {
				p.appendChild( document.createTextNode( ', ' ) );
			}
                }
		p.appendChild( document.createTextNode( '.' ) );
		var info = getElementsByClassName( file.parentNode, 'div', 'fullMedia' )[0];
		if( info ) {
			info.appendChild( p );
		}
	}
}
$( SVGThumbs );