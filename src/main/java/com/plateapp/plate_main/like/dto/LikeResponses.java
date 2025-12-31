package com.plateapp.plate_main.like.dto;

public class LikeResponses {
  public record ToggleLikeResponse(boolean liked, long likeCount) {}
}
