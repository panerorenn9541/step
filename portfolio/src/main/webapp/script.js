// Copyright 2019 Google LLC
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

window.onload = function() {
  createMap()
};

function createMap() {
  const map = new google.maps.Map(
      document.getElementById('map'),
      {center: {lat: 37.422, lng: -122.084}, zoom: 16});
}
