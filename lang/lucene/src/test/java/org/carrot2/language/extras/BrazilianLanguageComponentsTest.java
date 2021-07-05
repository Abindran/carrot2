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
package org.carrot2.language.extras;

import java.io.IOException;

public class BrazilianLanguageComponentsTest extends AbstractLanguageComponentsTest {
  public BrazilianLanguageComponentsTest() throws IOException {
    super(
        BrazilianLanguageComponents.NAME,
        new String[] {"diversas", "qualquer"},
        new String[][] {
          {"bobalhões", "bobalho"},
          {"quintão", "quinta"},
        });
  }
}
