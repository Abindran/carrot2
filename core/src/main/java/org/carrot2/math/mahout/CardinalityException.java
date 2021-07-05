/*
 * Carrot2 project.
 *
 * Copyright (C) 2002-2021, Dawid Weiss, Stanisław Osiński.
 * All rights reserved.
 *
 * Refer to the full license file "carrot2.LICENSE"
 * in the root folder of the repository checkout or at:
 * https://www.carrot2.org/carrot2.LICENSE
 */
package org.carrot2.math.mahout;

@SuppressWarnings("serial")
public class CardinalityException extends IllegalArgumentException {

  public CardinalityException(int expected, int cardinality) {
    super("Required cardinality " + expected + " but got " + cardinality);
  }
}
