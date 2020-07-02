package com.google.sps.data;

public final class Comment {
  private final long id;
  private final String message;

  public Comment(long id, String message) {
    this.message = message;
    this.id = id;
  }
}