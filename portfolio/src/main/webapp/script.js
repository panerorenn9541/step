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

/** Fetches comments from the server and adds them to the DOM. */
function loadComments() {
  fetch('/data').then(response => response.json()).then((comments) => {
    const commentListElement = document.getElementById('comment-list');
    comments.forEach((comment) => {
      commentListElement.appendChild(createCommentElement(comment));
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
  messageElement.innerText = comment.message;

  commentElement.appendChild(messageElement);
  return commentElement;
}
