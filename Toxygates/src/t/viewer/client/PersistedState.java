/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package t.viewer.client;

import java.util.logging.Logger;

import javax.annotation.Nullable;

import otgviewer.client.components.Screen;

/**
 * A piece of client state that can be individually stored and applied.
 * @param <T> The type of the state
 */
abstract public class PersistedState<T> {

  protected Logger logger;
  protected String storageKey;
  protected @Nullable T value = null;
  
  public PersistedState(String name, String storageKey) {
    logger = Logger.getLogger("PersistedState." + name);
    this.storageKey = storageKey;
  }
  
  public @Nullable T getValue() {
    return value;
  }
  
  /**
   * Serialise the state
   */
  protected @Nullable String pack(@Nullable T state) {
    if (state == null) {
      return null;
    }
    return doPack(state);
  }
  
  abstract protected @Nullable String doPack(T state);
  
  /**
   * Deserialise the state
   */
  protected @Nullable T unpack(@Nullable String state) {
    if (state == null) {
      return null;
    } 
    return doUnpack(state);    
  }
  
  abstract protected @Nullable T doUnpack(String state); 
  
  public void loadAndApply(StorageParser parser) {
    T state = unpack(parser.getItem(storageKey));
    value = state;
    onValueChange(state);
  }
  
  public void store(StorageParser parser, @Nullable T state) {
    String sstate = pack(state);
    if (sstate != null) {
      parser.setItem(storageKey, sstate);
    } else {
      parser.clearItem(storageKey);
    }
  }
  
  /**
   * Optionally override this method to get callbacks when the value changes
   */
  protected void onValueChange(@Nullable T state) { }
  
  /**
   * Convenience method
   * @param screen
   * @param newState
   */
  public void changeAndPersist(Screen screen, @Nullable T newState) {
    changeAndPersist(screen.manager().getParser(), newState);
  }
  
  /**
   * Change the value of this state as a result of e.g. 
   * a user action, persisting and then applying it.
   * @param newState
   */  
  public void changeAndPersist(StorageParser parser, @Nullable T newState) {
    logger.info("Changed");
    value = newState;
    store(parser, newState);
    onValueChange(newState);
  }  
}

