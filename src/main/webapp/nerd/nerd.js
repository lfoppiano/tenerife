var nerd = (function ($) {
    // for components view
    var responseJson = null;

    // for associating several entities to an annotation position (to support nbest mode visualisation)
    var entityMap = new Object();

    function defineBaseURL(ext) {
        var baseUrl = null;
        if ($(location).attr('href').indexOf("index.html") != -1)
            baseUrl = $(location).attr('href').replace("index.html", "service/" + ext);
        else
            baseUrl = $(location).attr('href') + "service/" + ext;
        return baseUrl;
    }

    function setBaseUrl(ext) {
        var baseUrl = defineBaseURL(ext);
        $('#gbdForm').attr('action', baseUrl);
    }

    $(document).ready(function () {
        $("#divServices").hide();
        $("#divAbout").show();

        $("#about").click(function () {
            $("#about").attr('class', 'section-active');
            $("#services").attr('class', 'section-non-active');

            $("#divServices").hide();
            $("#divAbout").show();
            return false;
        });

        $("#services").click(function () {
            $("#services").attr('class', 'section-active');
            $("#about").attr('class', 'section-non-active');

            $("#divServices").show();
            $("#divAbout").hide();

            return false;
        });

        setBaseUrl('data');
        $('#submitRequest').bind('click', submitQuery);
    });

    function submitQuery() {
        var data = new FormData();

        data.append('file', jQuery('#input')[0].files[0]);

        var urlLocal = $('#gbdForm').attr('action');

        $.ajax({
            url: urlLocal,
            data: data,
            cache: false,
            accept: "application/json",
            contentType: false,
            processData: false,
            type: 'POST',
            success: function (data) {
                processResponse(data)
            },
            error: showError()
        });

        $('#requestResult').html('<font color="grey">Requesting server...</font>');
    }

    function showError() {
        $('#requestResult').html("<font color='red'>Error encountered while requesting the server.</font>");
        responseJson = null;
    }

    function htmll(s) {
        return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    function processResponse(responseText, statusText) {
        if ((responseText == null) || (responseText.length == 0)) {
            $('#requestResult')
                .html("<font color='red'>Error encountered while receiving the server's answer: response is empty.</font>");
            return;
        }
        responseJson = responseText;
        paragraphs = responseJson.paragraphs;

        var display = '<div class=\"note-tabs\"> \
			<ul id=\"resultTab\" class=\"nav nav-tabs\"> \
		   		<li class="active"><a href=\"#navbar-fixed-annotation\" data-toggle=\"tab\">Annotations</a></li>\n';

        display += '<li><a href=\"#navbar-fixed-json\" data-toggle=\"tab\">Response</a></li> \
			</ul> \
			<div class="tab-content"> \
			<div class="tab-pane active" id="navbar-fixed-annotation">\n';

        display += '<pre style="background-color:#FFF;width:95%;" id="displayAnnotatedText">';

        display += '<table id="sentenceNER" style="width:100%;table-layout:fixed;" class="table">';
        display += '<tr style="background-color:#FFF;">';
        display += '<td style="font-size:small;width:60%;border:1px solid #CCC;">';
        var annotationId = 0;
        for (var i = 0; i < paragraphs.length; i++) {
            var paragraph = paragraphs[i];
            var lastMaxIndex = paragraph.text.length;

            var string = paragraph.text;
            display += '<span id="paragraph-' + i + '">';

            var entities = paragraph.entities;
            if (entities) {
                var currentAnnotationIndex = entities.length - 1;
                for (var m = entities.length - 1; m >= 0; m--) {
                    var entity = entities[m];
                    var domains = entity.domains;
                    var label = null;

                    if (entity.type) {
                        label = entity.type;
                    } else if (domains && domains.length > 0) {
                        label = domains[0].toLowerCase();
                    } else {
                        label = entity.rawName;
                    }

                    var start = parseInt(entity.offsetStart, 10);
                    var end = parseInt(entity.offsetEnd, 10);

                    if (start > lastMaxIndex) {
                        // we have a problem in the initial sort of the entities
                        // the server response is not compatible with the client
                        console.log("Sorting of entities as present in the server's response not valid for this client.");
                    }
                    else if (start == lastMaxIndex) {
                        // the entity is associated to the previous map
                        entityMap[annotationId].push(entities[m]);
                    }
                    else if (end > lastMaxIndex) {
                        end = lastMaxIndex;
                        lastMaxIndex = start;
                        // the entity is associated to the previous map
                        entityMap[annotationId].push(entities[m]);
                    }
                    else {
                        string = string.substring(0, start)
                            + '<span id="annot-' + annotationId + '" info="' + i + '" rel="popover" data-color="' + label + '">'
                            + '<span class="label ' + label + '" style="cursor:hand;cursor:pointer;" >'
                            + string.substring(start, end) + '</span></span>' + string.substring(end, string.length + 1);
                        lastMaxIndex = start;
                        currentAnnotationIndex = m;
                        entityMap[annotationId] = [];
                        entityMap[annotationId].push(entities[m]);
                    }
                    annotationId = annotationId + 1;
                }
            }
            string = "<p>" + string.replace(/(\r\n|\n|\r)/gm, "</p><p>") + "</p>";
            //string = string.replace("<p></p>", "");

            display += '<p>' + string + '</p>';
            display += '</span>';
        }

        display += '</td>';
        display += '<td style="font-size:small;width:40%;padding:0 5px; border:0">' +
            '<span id="detailed_annot-0" /></td>';
        display += '</tr>';
        display += '</table>\n';

        display += '</pre>\n';

        display += '</div> \
					<div class="tab-pane " id="navbar-fixed-categories">\n';

        display += '<pre style="background-color:#FFF;width:50%;" id="displayCategories">';

        display += '</div> \
					<div class="tab-pane " id="navbar-fixed-json">\n';
        // JSON visualisation component
        // with pretty print
        display += "<pre class='prettyprint' id='jsonCode'>";

        display += "<pre class='prettyprint lang-json' id='xmlCode'>";
//        var testStr = vkbeautify.json(responseText);

        //display += htmll(testStr);

        display += "</pre>";
        display += '</div></div></div>';

        $('#requestResult').html(display);
        window.prettyPrint && prettyPrint();

        for (var key in entityMap) {
            if (entityMap.hasOwnProperty(key)) {
                $('#annot-' + key).bind('hover', viewEntity);
                $('#annot-' + key).bind('click', viewEntity);
            }
        }
        $('#detailed_annot-0').hide();
    }


    const wikimediaURL_EN = 'https://en.wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&pithumbsize=200&pageids=';
    const wikimediaURL_FR = 'https://fr.wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&pithumbsize=200&pageids=';
    const wikimediaURL_DE = 'https://de.wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&pithumbsize=200&pageids=';

    var imgCache = {};

    window.lookupWikiMediaImage = function (wikipedia, lang) {
        // first look in the local cache
        if (lang + wikipedia in imgCache) {
            var imgUrl = imgCache[lang + wikipedia];
            var document = (window.content) ? window.content.document : window.document;
            var spanNode = document.getElementById("img-" + wikipedia);
            spanNode.innerHTML = '<img src="' + imgUrl + '"/>';
        } else {
            // otherwise call the wikipedia API
            var theUrl = null;
            if (lang === 'fr')
                theUrl = wikimediaURL_FR + wikipedia;
            else if (lang === 'de')
                theUrl = wikimediaURL_DE + wikipedia;
            else
                theUrl = wikimediaURL_EN + wikipedia;

            // note: we could maybe use the en crosslingual correspondance for getting more images in case of non-English pages
            $.ajax({
                url: theUrl,
                jsonp: "callback",
                dataType: "jsonp",
                xhrFields: {withCredentials: true},
                success: function (response) {
                    var document = (window.content) ? window.content.document : window.document;
                    var spanNode = document.getElementById("img-" + wikipedia);
                    if (response.query && spanNode) {
                        if (response.query.pages[wikipedia]) {
                            if (response.query.pages[wikipedia].thumbnail) {
                                var imgUrl = response.query.pages[wikipedia].thumbnail.source;
                                spanNode.innerHTML = '<img src="' + imgUrl + '"/>';
                                // add to local cache for next time
                                imgCache[lang + wikipedia] = imgUrl;
                            }
                        }
                    }
                }
            });
        }
    };

    function viewEntity() {

        var localID = $(this).attr('id');
        var paragraphID = $(this).attr('info');

        console.log("fetching anotationID: " + $(this).attr("id") + ", paragraphID: " + $(this).attr('info'));

        var ind1 = localID.indexOf('-');
        var localEntityNumber = parseInt(localID.substring(ind1 + 1, localID.length));

        var paragraph = paragraphs[paragraphID];

        if (paragraph.entities == null) {
            return null;
        }

        if ((entityMap[localEntityNumber] == null) || (entityMap[localEntityNumber].length == 0)) {
            // this should never be the case
            console.log("Error for visualising annotation with id " + localEntityNumber
                + ", empty list of entities");
        }

        var lang = 'en'; //default
        var language = paragraph.language;
        if (language) {
            lang = language.lang;
        }
        var string = "";
        for (var entityListIndex = entityMap[localEntityNumber].length - 1;
             entityListIndex >= 0;
             entityListIndex--) {

            //var entity = responseJson.entities[localEntityNumber];
            var entity = entityMap[localEntityNumber][entityListIndex];
            var domains = entity.domains;
            var type = entity.type;

            var colorLabel = null;
            if (type) {
                colorLabel = type;
            } else if (domains && domains.length > 0) {
                colorLabel = domains[0].toLowerCase();
            } else {
                colorLabel = entity.rawName;
            }

            var subType = entity.subtype;
            var conf = entity.nerd_score;
            var definitions = entity.definitions;
            var wikipedia = entity.wikipediaExternalRef;
            var content = entity.rawName;
            var normalized = entity.preferredTerm;

            var sense = null;
            if (entity.sense)
                sense = entity.sense.fineSense;

            string += "<div class='info-sense-box " + colorLabel +
                "'><h4 style='color:#FFF;padding-left:10px;'>" + content.toUpperCase() + "</h4>";
            string += "<div class='container-fluid' style='background-color:#F9F9F9;color:#70695C;border:padding:5px;margin-top:5px;'>" +
                "<table style='width:100%;background-color:#fff;border:0px'><tr style='background-color:#fff;border:0px;'><td style='background-color:#fff;border:0px;'>";

            if (type) {
                string += "<p>Type: <b>" + type + "</b></p>";
            }

            if (sense) {
                // to do: cut the sense string to avoid a string too large
                if (sense.length <= 20)
                    string += "<p>Sense: <b>" + sense + "</b></p>";
                else {
                    var ind = sense.indexOf('_');
                    if (ind != -1) {
                        string += "<p>Sense: <b>" + sense.substring(0, ind + 1) + "<br/>" +
                            sense.substring(ind + 1, sense.length) + "</b></p>";
                    }
                    else
                        string += "<p>Sense: <b>" + sense + "</b></p>";
                }
            }
            if (normalized)
                string += "<p>Normalized: <b>" + normalized + "</b></p>";

            if (domains && domains.length > 0) {
                string += "<p>Domains: <b>";
                for (var i = 0; i < domains.length; i++) {
                    if (i != 0)
                        string += ", ";
                    string += domains[i];
                }
                string += "</b></p>";
            }

            string += "<p>conf: <i>" + conf + "</i></p>";
            string += "</td><td style='align:right;bgcolor:#fff'>";

            string += '<span id="img-' + wikipedia + '"><script type="text/javascript">lookupWikiMediaImage("' + wikipedia + '", "' + lang + '")</script></span>';

            string += "</td></tr></table>";

            if ((definitions != null) && (definitions.length > 0)) {
                var localHtml = wiki2html(definitions[0]['definition'], lang);
                string += "<p><div class='wiky_preview_area2'>" + localHtml + "</div></p>";
            }
            if (wikipedia != null) {
                string += '<p>Reference: ';
                if (wikipedia != null) {
                    string += '<a href="http://' + lang + '.wikipedia.org/wiki?curid=' +
                        wikipedia +
                        '" target="_blank"><img style="max-width:28px;max-height:22px;margin-top:5px;" ' +
                        ' src="resources/img/wikipedia.png"/></a>';
                }
                string += '</p>';
            }

            string += "</div></div>";
        }
        $('#detailed_annot-0').html(string);
        $('#detailed_annot-0').show();
    }


    var queryTemplate = {
        "text": "", "language": {"lang": "en"}, "entities": [], "onlyNER": false, "resultLanguages": ["de", "fr"],
        "nbest": false, "sentence": false, "format": "JSON",
        "customisation": "generic"
    };

    function convert2NicelyTabulated(jsonParses, indexSentence) {
        var result =
            '<table class="table table-condensed" style="border-width:0px;font-size:small;background-color:#FEE9CC;">';
        connlParse = jsonParses[indexSentence].parse;
        // we remove the first index
        var lines = connlParse.split(/\r?\n/);
        for (var line in lines) {
            if (lines[line].trim().length == 0)
                continue;
            result += '<tr style="align:left;border-width: 0px;font-size:small;background-color:#FEE9CC;">';
            var tokens = lines[line].split(/\s/);
            var n = 0;
            for (var token in tokens) {
                if (tokens[token].trim().length == 0)
                    continue;
                result += '<td style="align:left;border-width: 0px;font-size:small;background-color:#FEE9CC;">';
                if (n == 1) {
                    result += '<b>' + tokens[token] + '</b>';
                }
                else if (n == 3) {
                    if (tokens[token] == '.')
                        result += 'DOT';
                    else if (tokens[token] == ',')
                        result += 'PUNCT';
                    else
                        result += tokens[token];
                }
                else {
                    result += tokens[token];
                }
                result += '</td>';
                n++;
            }
            result += '</tr>';
        }

        result += '</table>';
        return result;
    }


})(jQuery);