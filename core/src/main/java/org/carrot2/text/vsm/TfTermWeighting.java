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
package org.carrot2.text.vsm;

import org.carrot2.attrs.AttrComposite;

/** Calculates term-document matrix element values based on Term Frequency. */
public class TfTermWeighting extends AttrComposite implements TermWeighting {
  public double calculateTermWeight(int termFrequency, int documentFrequency, int documentCount) {
    return termFrequency;
  }
}
