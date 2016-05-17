package ch.rgw.lucinda;

import java.util.Map;

/**
 * Created by gerry on 11.05.16.
 */
public interface Handler {
  public void signal(Map<String,Object> detail);
}
