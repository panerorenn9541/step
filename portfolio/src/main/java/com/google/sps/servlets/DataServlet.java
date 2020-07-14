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

package com.google.sps.servlets;

import static com.google.sps.servlets.Constants.cType;
import static com.google.sps.servlets.Constants.encoding;
import static com.google.sps.servlets.Constants.says;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.Sentiment;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that returns some example content. TODO: modify this file to handle comments data */
@WebServlet("/data")
public class DataServlet extends HttpServlet {
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setCharacterEncoding(encoding);
    Query query = new Query("comment");
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);
    String language = getRequestParameterOrDefault(request, "lang", "");

    ArrayList<Comment> comments = new ArrayList<>();
    for (Entity entity : results.asIterable()) {
      long id = entity.getKey().getId();
      String message = (String) entity.getProperty("message");
      message = says + message;
      message = message.trim();
      Translate translate = TranslateOptions.getDefaultInstance().getService();
      Translation translation =
          translate.translate(message, Translate.TranslateOption.targetLanguage(language));
      String translatedText = translation.getTranslatedText();

      String name = (String) entity.getProperty("name");
      String sentiment = (String) entity.getProperty("sentiment");
      Comment comment = new Comment(id, name, translatedText, sentiment);
      comments.add(comment);
    }

    response.setContentType("cType");
    response.getWriter().println(convertToJson(comments));
  }

  class Comment {
    private final long id;
    private final String name;
    private final String message;
    private final String sentiment;

    public Comment(long id, String name, String message, String sentiment) {
      this.id = id;
      this.name = name;
      this.message = message;
      this.sentiment = sentiment;
    }
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Get the input from the form.
    String text = getRequestParameterOrDefault(request, "text-input", "");
    String name = getRequestParameterOrDefault(request, "name", "");

    // Calculate sentiment score
    Document doc = Document.newBuilder().setContent(text).setType(Document.Type.PLAIN_TEXT).build();
    LanguageServiceClient languageService = LanguageServiceClient.create();
    Sentiment sentiment = languageService.analyzeSentiment(doc).getDocumentSentiment();
    float scoref = sentiment.getScore();
    String score = Float.toString(scoref);
    languageService.close();

    // Respond with the result.
    response.setContentType("text/html;");
    response.getWriter().println(text);

    Entity commentEntity = new Entity("comment");
    commentEntity.setProperty("message", text);
    commentEntity.setProperty("name", name);
    commentEntity.setProperty("sentiment", score);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(commentEntity);

    response.sendRedirect("/index.html");
  }

  private String convertToJson(ArrayList<Comment> cmts) {
    Gson gson = new Gson();
    String json = gson.toJson(cmts);
    return json;
  }

  /**
   * @return the request parameter, or the default value if the parameter
   *         was not specified by the client
   */
  private String getRequestParameterOrDefault(
      HttpServletRequest request, String name, String defaultValue) {
    String value = request.getParameter(name);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }
}
