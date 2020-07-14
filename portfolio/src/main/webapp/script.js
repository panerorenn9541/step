// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * Adds a random Seinfeld quote to the page.
 */
function addRandomQuote() {
  const quotes = [
    'Tuesday has no feel. Monday has a feel, Friday has a feel, Sunday has a feel...',
    'Jerry, just remember, its not a lie if you believe it.',
    'You know I always wanted to pretend I was an architect.',
    'I don\'t think George has ever thought he\'s better than anybody.'
  ];

  // Pick a random quote.
  const quote = quotes[Math.floor(Math.random() * quotes.length)];

  // Add it to the page.
  const quoteContainer = document.getElementById('quote-container');
  quoteContainer.innerText = quote;
}

window.onload = function() {
  loadComments()
  createMap()
};
/** Fetches comments from the server and adds them to the DOM. */
function loadComments() {
  const limit = document.getElementById('commentPages').value;
  const commentsPerPage = 5;
  counter = 0;

  const language = document.getElementById('commentLanguage').value;

  fetch('/data?lang=' + language)
      .then(response => response.json())
      .then((comments) => {
        const commentListElement = document.getElementById('comment-list');
        while (commentListElement.firstChild) {
          commentListElement.removeChild(commentListElement.lastChild);
        }
        comments.forEach((comment) => {
          if (counter < limit * commentsPerPage &&
              counter >= (limit - 1) * commentsPerPage) {
            commentListElement.appendChild(createCommentElement(comment));
            ++counter;
          }
        })
      });
}

/**
 * Creates an element that represents a comment, including its delete button.
 */
function createCommentElement(comment) {
  const commentElement = document.createElement('li');
  commentElement.className = 'comment';

  const messageElement = document.createElement('span');
  messageElement.innerText = `${comment.name} ${
      comment.message} (Sentiment Score: ${comment.sentiment})`;

  const deleteButtonElement = document.createElement('button');
  deleteButtonElement.innerText = 'Delete';
  deleteButtonElement.addEventListener('click', () => {
    deleteComment(comment);

    // remove the comment from the DOM.
    commentElement.remove();
  });

  commentElement.appendChild(messageElement);
  commentElement.appendChild(deleteButtonElement);
  return commentElement;
}

/** Tells the server to delete the comment. */
function deleteComment(comment) {
  const params = new URLSearchParams();
  params.append('id', comment.id);
  fetch('/delete-data', {method: 'POST', body: params});
}

function createMap() {
  const map = new google.maps.Map(
      document.getElementById('map'),
      {center: {lat: 46.8797, lng: -110.3626}, zoom: 3});
  fetch('/report').then(response => response.json()).then((reports) => {
    var heatmapData = []
    reports.forEach((report) => {
      heatmapData.push({
        location: new google.maps.LatLng(report.lat, report.lng),
        weight: report.active
      });
    });
    heatmap = new google.maps.visualization.HeatmapLayer(
        {data: heatmapData, dissipating: false, map: map});
  });

  var sunnyvale = {lat: 37.4030, lng: -122.0326};
  var sunnyvaleString = '<div id="content">' +
      '<div id="siteNotice">' +
      '</div>' +
      '<h1 id="firstHeading" class="firstHeading">Sunnyvale</h1>' +
      '<div id="bodyContent">' +
      '<p><b>Sunnyvale</b> is where I was going to do my Google internship!</p>' +
      '</div>' +
      '</div>';
  var sunnyvaleInfowindow =
      new google.maps.InfoWindow({content: sunnyvaleString});
  var sunnyvaleMarker = new google.maps.Marker(
      {position: sunnyvale, map: map, title: 'Sunnyvale'});
  sunnyvaleMarker.addListener('click', function() {
    sunnyvaleInfowindow.open(map, sunnyvaleMarker);
  });

  var coronado = {lat: 32.6859, lng: -117.1831};
  var coronadoString = '<div id="content">' +
      '<div id="siteNotice">' +
      '</div>' +
      '<h1 id="firstHeading" class="firstHeading">Coronado</h1>' +
      '<div id="bodyContent">' +
      '<p><b>Coronado</b> is where I was born</p>' +
      '</div>' +
      '</div>';
  var coronadoInfowindow =
      new google.maps.InfoWindow({content: coronadoString});
  var coronadoMarker =
      new google.maps.Marker({position: coronado, map: map, title: 'Coronado'});
  coronadoMarker.addListener('click', function() {
    coronadoInfowindow.open(map, coronadoMarker);
  });

  var barca = {lat: 41.3809, lng: 2.1228};
  var barcaString = '<div id="content">' +
      '<div id="siteNotice">' +
      '</div>' +
      '<h1 id="firstHeading" class="firstHeading">Camp Nou</h1>' +
      '<div id="bodyContent">' +
      '<p><b>The Camp Nou</b> is where my favorite team FC Barcelona plays</p>' +
      '</div>' +
      '</div>';
  var barcaInfowindow = new google.maps.InfoWindow({content: barcaString});
  var barcaMarker =
      new google.maps.Marker({position: barca, map: map, title: 'Barca'});
  barcaMarker.addListener('click', function() {
    barcaInfowindow.open(map, barcaMarker);
  });

  var watford = {lat: 51.6565, lng: -0.3903};
  var watfordString = '<div id="content">' +
      '<div id="siteNotice">' +
      '</div>' +
      '<h1 id="firstHeading" class="firstHeading">Watford</h1>' +
      '<div id="bodyContent">' +
      '<p><b>Watford</b> is where I stayed during my time in England</p>' +
      '</div>' +
      '</div>';
  var watfordInfowindow = new google.maps.InfoWindow({content: watfordString});
  var watfordMarker =
      new google.maps.Marker({position: watford, map: map, title: 'Watford'});
  watfordMarker.addListener('click', function() {
    watfordInfowindow.open(map, watfordMarker);
  });

  var utrecht = {lat: 52.0907, lng: 5.1214};
  var utrechtString = '<div id="content">' +
      '<div id="siteNotice">' +
      '</div>' +
      '<h1 id="firstHeading" class="firstHeading">Utrecht</h1>' +
      '<div id="bodyContent">' +
      '<p><b>Utrecht</b> is where I stayed when I went to the Netherlands.' +
      'The waffles and skewers are amazing there!</p>' +
      '</div>' +
      '</div>';
  var utrechtInfowindow = new google.maps.InfoWindow({content: utrechtString});
  var utrechtMarker =
      new google.maps.Marker({position: utrecht, map: map, title: 'Utrecht'});
  utrechtMarker.addListener('click', function() {
    utrechtInfowindow.open(map, utrechtMarker);
  });

  var porto = {lat: 41.1433, lng: -8.6103};
  var portoString = '<div id="content">' +
      '<div id="siteNotice">' +
      '</div>' +
      '<h1 id="firstHeading" class="firstHeading">Porto</h1>' +
      '<div id="bodyContent">' +
      '<p><b>Porto</b> is the city I stayed in when I went to Portugal.' +
      'The view from the Ribeira is beautiful!</p>' +
      '</div>' +
      '</div>';
  var portoInfowindow = new google.maps.InfoWindow({content: portoString});
  var portoMarker =
      new google.maps.Marker({position: porto, map: map, title: 'Porto'});
  portoMarker.addListener('click', function() {
    portoInfowindow.open(map, portoMarker);
  });
}