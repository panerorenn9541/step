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
import static com.google.sps.servlets.Constants.stats;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Returns case data as a JSON array, e.g. [{"lat": 38.4404675, "lng": -122.7144313}] */
@WebServlet("/report")
public class CasesDataServlet extends HttpServlet {
  private String json;

  @Override
  public void init() {
    Collection<Report> reports = new ArrayList<>();

    Scanner scanner = new Scanner(getServletContext().getResourceAsStream(stats));
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();
      String[] cells = line.split(",");

      double lat = Double.parseDouble(cells[5]);
      double lng = Double.parseDouble(cells[6]);
      int active = Integer.parseInt(cells[10]);

      reports.add(new Report(lat, lng, active));
    }
    scanner.close();
    Gson gson = new Gson();
    json = gson.toJson(reports);
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType(cType);
    response.getWriter().println(json);
  }

  /** Represents a case at a specific lat lng point. */
  class Report {
    private double lat;
    private double lng;
    private int active;

    public Report(double lat, double lng, int active) {
      this.lat = lat;
      this.lng = lng;
      this.active = active;
    }
  }
}