/*
 * Copyright (C) 2013 Jason Taylor.
 * Released as open-source under the Apache License, Version 2.0.
 * 
 * ============================================================================
 * | Joise
 * ============================================================================
 * 
 * Copyright (C) 2013 Jason Taylor
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * ============================================================================
 * | Accidental Noise Library
 * | --------------------------------------------------------------------------
 * | Joise is a derivative work based on Josua Tippetts' C++ library:
 * | http://accidentalnoise.sourceforge.net/index.html
 * ============================================================================
 * 
 * Copyright (C) 2011 Joshua Tippetts
 * 
 *   This software is provided 'as-is', without any express or implied
 *   warranty.  In no event will the authors be held liable for any damages
 *   arising from the use of this software.
 * 
 *   Permission is granted to anyone to use this software for any purpose,
 *   including commercial applications, and to alter it and redistribute it
 *   freely, subject to the following restrictions:
 * 
 *   1. The origin of this software must not be misrepresented; you must not
 *      claim that you wrote the original software. If you use this software
 *      in a product, an acknowledgment in the product documentation would be
 *      appreciated but is not required.
 *   2. Altered source versions must be plainly marked as such, and must not be
 *      misrepresented as being the original software.
 *   3. This notice may not be removed or altered from any source distribution.
 */

package com.sudoplay.joise.module;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import com.sudoplay.joise.JoiseException;
import com.sudoplay.joise.ModuleInstanceMap;
import com.sudoplay.joise.ModuleMap;
import com.sudoplay.joise.ModulePropertyMap;

public abstract class Module {

  public static final long DEFAULT_SEED = 10000;
  public static final int MAX_SOURCES = 10;

  protected double spacing = 0.0001;

  public abstract double get(double x, double y);

  public abstract double get(double x, double y, double z);

  public abstract double get(double x, double y, double z, double w);

  public abstract double get(double x, double y, double z, double w, double u,
      double v);

  protected static AtomicInteger nextId = new AtomicInteger();

  private String id = setId();

  protected String setId() {
    return "func_" + nextId.incrementAndGet();
  }

  public String getId() {
    return id;
  }

  public ModuleMap getModuleMap() {
    ModuleMap map = new ModuleMap();
    _writeToMap(map);
    return map;
  }

  public void writeToMap(ModuleMap map) {
    if (map.contains(id)) {
      return;
    }
    _writeToMap(map);
  }

  protected abstract void _writeToMap(ModuleMap map);

  public abstract Module buildFromPropertyMap(ModulePropertyMap props,
      ModuleInstanceMap map);

  public void setDerivativeSpacing(double spacing) {
    this.spacing = spacing;
  }

  public double getDX(double x, double y) {
    return (get(x - spacing, y) - get(x + spacing, y)) / spacing;
  }

  public double getDY(double x, double y) {
    return (get(x, y - spacing) - get(x, y + spacing)) / spacing;
  }

  public double getDX(double x, double y, double z) {
    return (get(x - spacing, y, z) - get(x + spacing, y, z)) / spacing;
  }

  public double getDY(double x, double y, double z) {
    return (get(x, y - spacing, z) - get(x, y + spacing, z)) / spacing;
  }

  public double getDZ(double x, double y, double z) {
    return (get(x, y, z - spacing) - get(x, y, z + spacing)) / spacing;
  }

  public double getDX(double x, double y, double z, double w) {
    return (get(x - spacing, y, z, w) - get(x + spacing, y, z, w)) / spacing;
  }

  public double getDY(double x, double y, double z, double w) {
    return (get(x, y - spacing, z, w) - get(x, y + spacing, z, w)) / spacing;
  }

  public double getDZ(double x, double y, double z, double w) {
    return (get(x, y, z - spacing, w) - get(x, y, z + spacing, w)) / spacing;
  }

  public double getDW(double x, double y, double z, double w) {
    return (get(x, y, z, w - spacing) - get(x, y, z, w + spacing)) / spacing;
  }

  public double getDX(double x, double y, double z, double w, double u, double v) {
    return (get(x - spacing, y, z, w, u, v) - get(x + spacing, y, z, w, u, v))
        / spacing;
  }

  public double getDY(double x, double y, double z, double w, double u, double v) {
    return (get(x, y - spacing, z, w, u, v) - get(x, y + spacing, z, w, u, v))
        / spacing;
  }

  public double getDZ(double x, double y, double z, double w, double u, double v) {
    return (get(x, y, z - spacing, w, u, v) - get(x, y, z + spacing, w, u, v))
        / spacing;
  }

  public double getDW(double x, double y, double z, double w, double u, double v) {
    return (get(x, y, z, w - spacing, u, v) - get(x, y, z, w + spacing, u, v))
        / spacing;
  }

  public double getDU(double x, double y, double z, double w, double u, double v) {
    return (get(x, y, z, w, u - spacing, v) - get(x, y, z, w, u + spacing, v))
        / spacing;
  }

  public double getDV(double x, double y, double z, double w, double u, double v) {
    return (get(x, y, z, w, u, v - spacing) - get(x, y, z, w, u, v + spacing))
        / spacing;
  }

  protected void assertMaxSources(int index) {
    if (index < 0 || index >= MAX_SOURCES) {
      throw new IllegalArgumentException("expecting index < " + MAX_SOURCES
          + " but was " + index);
    }
  }

  /**
   * Read a scalar property from the provided property map and set the property
   * in this module using reflection to call the supplied method name with the
   * retrieved property passed as an argument. If the scalar is a module name,
   * the module is retrieved from the provided {@link ModuleInstanceMap} and set
   * using the reflected method provided.
   * 
   * @param name
   * @param methodName
   * @param props
   * @param map
   * 
   * @throws JoiseException
   *           if there is an error with the retrieval or setting of the
   *           property
   */
  protected void readScalar(String name, String methodName,
      ModulePropertyMap props, ModuleInstanceMap map) {

    try {
      if (props.isModuleID(name)) {
        Method method = getClass().getMethod(methodName, Module.class);
        method.invoke(this, new Object[] { map.get(props.get(name)) });
      } else {
        Method method = getClass().getMethod(methodName, double.class);
        method.invoke(this, new Object[] { props.getAsDouble(name) });
      }
    } catch (Exception e) {
      throw new JoiseException(e);
    }

  }

  /**
   * Write a scalar property to the provided property map. If the scalar is a
   * module, {@link #_writeToMap(ModuleMap)} is called on the scalar's module.
   * 
   * @param key
   * @param scalar
   * @param props
   * @param map
   */
  protected void writeScalar(String key, ScalarParameter scalar,
      ModulePropertyMap props, ModuleMap map) {

    props.put(key, scalar);
    if (scalar != null && scalar.isModule()) {
      scalar.getModule()._writeToMap(map);
    }

  }

  /**
   * Read an enum property from the provided property map and set the property
   * in this module using reflection to call the supplied method name with the
   * retrieved property passed as an argument.
   * 
   * @param name
   * @param methodName
   * @param c
   * @param props
   */
  protected <T extends Enum<T>> void readEnum(String name, String methodName,
      Class<T> c, ModulePropertyMap props) {

    try {
      Method method = getClass().getMethod(methodName, c);
      T _enum = Enum.valueOf(c, props.get(name).toString().toUpperCase());
      method.invoke(this, new Object[] { _enum });
    } catch (Exception e) {
      throw new JoiseException(e);
    }

  }

  /**
   * Write an enum property to the provided property map. The enum is converted
   * to lower-case.
   * 
   * @param key
   * @param _enum
   * @param props
   */
  protected void writeEnum(String key, Enum<?> _enum, ModulePropertyMap props) {

    props.put(key, _enum.toString().toLowerCase());

  }

  /**
   * Read a long property from the provided property map and set the property in
   * this module using reflection to call the supplied method name with the
   * retrieved property passed as an argument.
   * 
   * @param key
   * @param methodName
   * @param props
   */
  protected void readLong(String key, String methodName, ModulePropertyMap props) {

    try {
      Method method = getClass().getMethod(methodName, long.class);
      method.invoke(this, new Object[] { props.getAsLong(key) });
    } catch (Exception e) {
      throw new JoiseException(e);
    }

  }

  /**
   * Write a long property to the provided property map.
   * 
   * @param key
   * @param value
   * @param props
   */
  protected void writeLong(String key, long value, ModulePropertyMap props) {

    props.put(key, value);

  }

  /**
   * Read a double property from the provided property map and set the property
   * in this module using reflection to call the supplied method name with the
   * retrieved property passed as an argument.
   * 
   * @param key
   * @param methodName
   * @param props
   */
  protected void readDouble(String key, String methodName,
      ModulePropertyMap props) {

    try {
      Method method = getClass().getMethod(methodName, double.class);
      method.invoke(this, new Object[] { props.getAsDouble(key) });
    } catch (Exception e) {
      throw new JoiseException(e);
    }

  }

  /**
   * Write a double property to the provided property map.
   * 
   * @param key
   * @param value
   * @param props
   */
  protected void writeDouble(String key, double value, ModulePropertyMap props) {

    props.put(key, value);

  }

  /**
   * Read a boolean property from the provided property map and set the property
   * in this module using reflection to call the supplied method name with the
   * retrieved property passed as an argument.
   * 
   * @param key
   * @param methodName
   * @param props
   */
  protected void readBoolean(String key, String methodName,
      ModulePropertyMap props) {

    try {
      Method method = getClass().getMethod(methodName, boolean.class);
      method.invoke(this, new Object[] { props.getAsBoolean(key) });
    } catch (Exception e) {
      throw new JoiseException(e);
    }

  }

  /**
   * Write a boolean property to the provided property map.
   * 
   * @param key
   * @param value
   * @param props
   */
  protected void writeBoolean(String key, boolean value, ModulePropertyMap props) {

    props.put(key, String.valueOf(value));

  }
}
