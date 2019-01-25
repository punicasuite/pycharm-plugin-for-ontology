(function (root, factory) {
    if ( typeof define === 'function' && define.amd )
    {
        define(['jquery'], factory);
    }
    else if ( typeof exports === 'object' )
    {
        module.exports = factory(require('jquery'));
    }
    else
    {
        factory(root.jQuery);
    }
}(this, function( $ ) {

    $.formelements = {
        version: "0.0.9"
    };

    var methods = {
        init: init,
        destroy: function() {
            if( this.hasClass( 'formelements_hide' ) ) {
                var settings = this.formelements('settings');
                this.removeData('formelements-settings');
                this.parent().find( "> *" ).not( this ).remove();
                this.unwrap();
                this.removeClass( "formelements_hide" );
                this.unbind( "sel_open", settings.sel_open );
                this.unbind( "sel_close", settings.sel_close );
                this.unbind( "sel_open", form_select_tsb_update );
            }
            return this;
        },
        disable: function() {
            this.attr( "disabled", true ).parent().addClass( "formelements_disabled" );
            return this;
        },
        enable: function() {
            this.attr( "disabled", false ).parent().removeClass( "formelements_disabled" );
            return this;
        },
        // Update the tinyscrollbar
        updateScroll: function(){
            var optwrap = this.closest('.formelements_item').find('.formelements_listwrap' );
            optwrap.data('tsb').update();
        },
        // Destroy and recreate the dropdown with the same options
        refresh: function(){
            var settings = this.formelements('settings');
            this.formelements('destroy').formelements(settings);
        },
        // Returns selected options from the selectbox
        selected: function(){
            var $select = $(this);
            var $opts = $select.find('option');
            return $opts.filter(':selected');
        },
		isEmpty: function()
		{
			return $(this).formelements('selected').not('.placeholder').length === 0;
		},
        // Returns the settings object
        settings: function() {
            var settings = this.data('formelements-settings');
            if( typeof( settings ) == "object" && settings !== null ) {
                return settings;
            }
            return undefined;
        }
    };

    $.fn.formelements = function( method ) {
        if( methods[method] ) {
            return methods[ method ].apply( this, Array.prototype.slice.call( arguments, 1 ) );
        }else if( typeof method === 'object' || ! method ) {
            return methods.init.apply( this, arguments );
        }else {
            $.error( 'Method ' + method + ' does not exist on jQuery.formelements' );
        }
    };

    function init( options ) {
        var settings = $.extend( {
            'classlist': "formelements_item",
            'sel_multiselect_control': false,
            'sel_tinyscroll': false,
			'sel_enable_clear': false,
            'sel_searchbar': false,
            'sel_searchbar_focus': true,
            'text_search_placeholder': "Search...",
            'sel_delegate_click': false,
            'sel_label': function( $li, $input, index ) {
            },
            'sel_open': function( e, dd ) {
                dd.optwrap.not( ".open" ).fadeIn( 200, function() {
                    if( dd.settings.sel_searchbar_focus ) {
                        $( this ).find( 'input' ).focus();
                    }
                });
                dd.optwrap.addClass( "open" );
                dd.wrap.addClass( "open" );
            },
            'sel_close': function( e, dd ) {
                dd.optwrap.fadeOut( 200, function() {
                    dd.optwrap.removeClass( "open" );
                    dd.wrap.removeClass( "open" );
                });
            },
            'search_function': function( search, $el ) {
                var text = $el.text();

                // hide the item if it is a placeholder, or if the searchvalue is not in its text
                if( $el.hasClass( 'placeholder' ) || text.toLowerCase().indexOf( search.toLowerCase() ) === -1 ) {
                    return 0;
                }
                return 1;
            },
            'render_display': function() {
                var $select = $(this);
                var $display = $select.closest('.formelements_select').find('.formelements_select_display');
                var $opts = $select.formelements('selected');

                var labels = [];
                $opts.each(function(){
                    labels.push( $(this).text() );
                });

                $display.html(labels.join(', '));
            }
        }, options );

        this.data('formelements-settings', settings);

        // Loop through every element the selector hit
        this.each( function() {

            // Set sel to the current element in the each
            var sel = $( this );

            // If we are already initialized, don't do it again!
            if( ! sel.hasClass( "formelements_hide" ) ) {

                /*
                 Radiobutton
                 */
                if( sel.is( "input[type='radio']" ) ) {
                    sel.addClass( "formelements_hide" );

                    sel.wrap( "<div class='formelements_radio' />" );
                    var wrap = sel.parent();

                    wrap.addClass( settings.classlist );

                    wrap.click( function(e) {
                        if( ! $(e.target).is('input') )
                        {
                            var $el = $(this).not('.formelements_disabled').not('input').find('input[type="radio"]');
                            $el.trigger('click', e);
                        }
                    });

                    /*
                        jQuery validate support
                    */
                    var $message = wrap.next();
                    if( $message.is('span') && typeof( $message.data('valmsg-for') ) == "string" )
                    {
                        wrap.append($message);
                    }

                    function rad_set()
                    {
                        var $radio = $( 'input[name="' + sel.attr( "name" ) + '"]' );

                        $radio.each( function() {
                            var $el = $( this );

                            if( $el.is( ":checked" ) )
                            {
                                $el.parent().addClass( "checked" );
                            }
                            else
                            {
                                $el.checked = false;
                                $el.parent().removeClass( "checked" );
                            }
                        });
                    }

                    sel.on( 'change', rad_set );
                    $.proxy( rad_set, sel )();
                }

                /*
                 Checkbox
                 */
                else if( sel.is( "input[type='checkbox']" ) ) {
                    sel.addClass( "formelements_hide" );

                    sel.wrap( "<div class='formelements_check' />" );
                    var wrap = sel.parent();

                    wrap.addClass( settings.classlist );

                    wrap.click( function( e ) {
                        if( ! $( e.target ).is( "input" ) && ! $( this ).hasClass( "formelements_disabled" ) ) {
                            $( this ).find("input[type='checkbox']").trigger('click');
                        }
                    });

                    /*
                        jQuery validate support
                    */
                    var $message = wrap.next();
                    if( $message.is('span') && typeof( $message.data('valmsg-for') ) == "string" )
                    {
                        wrap.append($message);
                    }

                    function chb_set() {
                        if( $( this ).is( ":checked" ) ) {
                            $( this ).parent().addClass( "checked" );
                        }else {
                            $( this ).parent().removeClass( "checked" );
                        }
                    }

                    sel.on( 'change', chb_set );
                    $.proxy( chb_set, sel )();
                }

                /*
                 Select
                 */
                else if( sel.is( "select" ) ) {
                    sel.addClass( "formelements_hide" );

                    var $opts = $( "<ul></ul>" );
                    sel.find( "option" ).each( function( index ) {
                        var $opt    = $( this );
                        var $el     = $( "<li></li>" );
                        var label   = $opt.html();

                        // Add a rel attribute to both the option and the list item
                        $opt.add( $el ).attr( 'rel', 'sel_' + index );
                        $el.html( label );

                        // Copy classlist from option to the li
                        var opt_class = $opt.attr( "class" );
                        $el.attr( "class", opt_class );

						if ( $opt.prop('disabled') )
						{
							$el.addClass('formelelemts_item_disabled');
						}

                        settings.sel_label( $el, $opt, index );

                        $opts.append( $el );
                    });

                    form_create_select( sel, $opts, settings );
                }

                // Trigger init event
                $( this ).trigger( 'init', $( this ).closest( '.formelements_item' ) );
            }
        });

        // Internal function to render the select element
        function form_create_select( element, opts, settings ) {
            var _this = this;
            var wrap = element.wrap( "<div class='formelements_select'></div>" ).parent();

            var multi = element.prop('multiple');

            wrap.addClass( settings.classlist );
            wrap.append( "<div class='formelements_select_display'></div>" );
            wrap.append( opts );

            var disp = wrap.find( ".formelements_select_display" );
            var optwrap = opts.wrap( '<div class="formelements_listwrap"></div>' ).parent();

            element.on( "sel_open", settings.sel_open );
            element.on( "sel_close", settings.sel_close );

            var click_outside = function(e){
                if( wrap.has( e.target ).length === 0 ) {
                    element.trigger( "sel_close", {wrap: wrap, optwrap: optwrap, settings: settings} );
                }
            };

            element.on( 'sel_open', function(){
                $( document ).bind( 'mouseup', click_outside);
            });

            element.on( 'sel_close', function(){
                $( document ).unbind( 'mouseup', click_outside);
            });

            /*
                jQuery validate support
            */
            var $message = wrap.next();
            if( $message.is('span') && typeof( $message.data('valmsg-for') ) == "string" )
            {
                wrap.append($message);
            }

            /*
                Option group code
            */
            var $groups = element.find('optgroup');
            if( $groups.length ) {
                $groups.each(function(){
                    var $el = $(this);
                    var label = $el.attr('label');
                    var $items = $el.children('option');
                    var $lis = $([]);

                    $items.each(function(){
                        var rel = $(this).attr('rel');
                        $lis = $lis.add( wrap.find('li[rel="' + rel + '"]') )
                    });

                    $lis.wrapAll('<li class="formelements_optgroup"><ul></ul></li>');
                    $lis.parent().before('<span class="formelements_optgroup_label">' + label + '</span>');
                });
            }

            /*
             Tiny scrollbar code
             */
            if( settings.sel_tinyscroll ) {
                wrap.addClass('formelements_tinyscrollbar');
                optwrap.show().css( 'visibility', 'hidden' );
                optwrap.prepend( '<div class="scrollbar"><div class="track"><div class="thumb"><div class="end"></div></div></div></div>' );
                opts.wrap( '<div class="viewport"><div class="overview"></div></div>' );
                optwrap.tinyscrollbar();

                setTimeout( function() {
                    optwrap.hide().css( 'visibility', 'visible' );
                }, 10 );

                element.on('sel_open', form_select_tsb_update);
            }

            /*
             Searchbar code
             */
            if( settings.sel_searchbar ) {
                optwrap.children().wrapAll( "<div class='formelements_select_contentwrap'></div>" );
                optwrap.prepend( "<div class='formelements_searchwrap'><input type='text' placeholder='" + settings.text_search_placeholder + "' /></div>" );
                optwrap.find( 'input' ).bind( "keyup change", function() {
                    var val = $( this ).val();
                    var $li = opts.find( 'li' ).removeClass( 'search_hidden' );

                    if( typeof( val ) === "string" && val.length > 0 ) {
                        $li.each( function() {
                            if( ! settings.search_function( val, $( this ) ) ) {
                                $( this ).addClass( 'search_hidden' );
                            }
                        } );
                    }

                    // If we use tiny scrollbar, update scrollbar size
                    if( settings.sel_tinyscroll ) {
                        setTimeout( function() {
                            opts.closest('.formelements_listwrap').tinyscrollbar_update();
                        }, 10 );
                    }

                });

                element.on( "sel_open", function( e, dd ) {
                    dd.optwrap.find( 'input' ).val( "" ).change();
                });
            }

			/*
			 Clear code
			 */
			if ( settings.sel_enable_clear )
			{
				var $clearbtn = $('<a class="formelements_clear" href="javascript:;">&#10005;</a>');
				wrap.append($clearbtn);
				$clearbtn.on('click', function()
				{
					element.val('').change();
				});
			}

            if( settings.sel_delegate_click ) {
                element.css({
                    'position': 'absolute',
                    'visibility': 'visible',
                    'opacity': 0,
                    'height': '100%',
                    'width': '100%',
                    'top': 0,
                    'left': 0,
                    'right': 0,
                    'bottom': 0
                });
            }

            /*
             Opening the dropdown
             */
            disp.click( function(e) {
                if( ! wrap.hasClass( "formelements_disabled" ) )
                {
                    if( ! settings.sel_delegate_click )
                    {
                        if( ! wrap.hasClass( 'open' ) )
                        {
                            element.trigger("sel_open", {wrap: wrap, optwrap: optwrap, settings: settings});
                        }
                        else
                        {
                            element.trigger( "sel_close", {wrap: wrap, optwrap: optwrap, settings: settings} );
                        }
                    }
                }
            });

            /*
             Selection code
             */
            opts.find( "li").not('.formelements_optgroup').click(function(e) {
                var $li = $( this );

				if ( $li.hasClass('formelelemts_item_disabled') )
				{
					return false;
				}

                if( ! multi ) {
                    element.trigger("sel_close", {wrap: wrap, optwrap: optwrap, settings: settings});
                }

                if( ! e.ctrlKey && settings.sel_multiselect_control ) {
                    opts.find( "li" ).removeClass('active');
                    element.find( ':selected').prop('selected', false);
                }

                if( $li.hasClass( 'active' ) && ! multi ) {
                    return false;
                }

                var $wrapper = $li.closest( '.formelements_select' );

                form_select_change( $wrapper, $li, true );
            });

            /*
             Change code
             */
            element.change(function(){
            	form_select_render( $(this) );
            });

            form_select_render(element);
        }

        function form_select_render($input) {
        	var $wrap = $input.closest( '.formelements_select' );
            var $display = $wrap.find( '.formelements_select_display' );
            var $opt = $input.find( ":selected" );

            $wrap.find('li.active').removeClass('active');
            $display.removeClass('placeholder');

            if( $opt.length === 0 ) {
            	$opt = $input.find('option').first();
                if( $opt.hasClass('placeholder') ) {
                    $wrap.find( '.formelements_select_display' ).addClass('placeholder');
                }
            }

            $opt.each(function(){
                var rel = $(this).attr( "rel" );
                var $li = $wrap.find( 'li[rel="' + rel + '"]' );
                form_select_change( $wrap, $li, false );
            });

			if ( $input.formelements('isEmpty') )
			{
				$wrap.addClass('formelements_empty');
			}
			else
			{
				$wrap.removeClass('formelements_empty');
			}
        }

        function form_select_change( wrap, $opt, change ) {
            var $opts = wrap.find( 'li' );
            var $input = wrap.find( 'select' );
            var multi = $input.prop('multiple');
            var $display = wrap.find( '.formelements_select_display' );
            var $selected = $input.find( ':selected' );

            if( ! multi ) {
                $opts.removeClass('active');
                $opt.addClass('active');

                $input.find('option[rel="' + $opt.attr('rel') + '"]').prop('selected', true);

                if ( $selected.hasClass('placeholder') ) {
                    $display.addClass('placeholder');
                } else {
                    $display.removeClass('placeholder');
                }
            }else {

            	// Selected option is already active, deactivate it!
                if( $opt.hasClass('active') && change == true ) {
                    $opt.removeClass('active');
                    $input.find('option[rel="' + $opt.attr('rel') + '"]').prop('selected', false);
                }else {
                    if( $opt.hasClass('placeholder') ) {
                        $opts.removeClass('active');
                        $input.find( ':selected').prop('selected', false);
                        $display.addClass('placeholder');
                    }

                    $opt.addClass('active');
                    $input.find('option[rel="' + $opt.attr('rel') + '"]').prop('selected', true);
                }

                // Noting is selected, re-render, setting the first option!
                if($opts.filter('.active').length == 0) {
                    form_select_render($input);
                }

                // If placeholder is selected, as well as an option, de-select the placeholder
                if( $opts.filter('.active').length > 1 ) {
                    var $ph = $input.find('.placeholder:selected');
                    if( $ph.length > 0 ) {
                    	$ph.prop('selected', false);
                    	$opts.filter('[rel="' + $ph.attr('rel') + '"]').removeClass('active');
                    }
                }
            }

            $.proxy(settings.render_display, $input)();

            if ( change != false ) {
                $input.change();
            }
        }

        // Return itself ( for easy chaining )
        return this;
    }

    function form_select_tsb_update()
    {
        var optwrap = $(this).closest('.formelements_select').find('.formelements_listwrap');
        optwrap.data('tsb').update();
    }

    return $;
}));