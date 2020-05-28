package com.github.libpq4s.api

import scala.beans.BeanProperty

case class ConnInfoOption(
  /** The name of the option */
  @BeanProperty keyword: String,
  /** The environment variable to fall back to*/
  @BeanProperty envVar: String,
  /** The compiled in option as a secondary fallback */
  @BeanProperty compiled: String,
  /** Current value, defined if known, otherwise None */
  @BeanProperty value: Option[String],
  /** Label for field in connect dialog */
  @BeanProperty label: String,
  /** Indicates how to display this field: "" for normal, "D" for debug, and "*" for password */
  @BeanProperty dispChar: Char,
  /** Field size in characters for connect dialog */
  @BeanProperty dispSize: Int
)
