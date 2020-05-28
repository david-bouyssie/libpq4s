package com.github.libpq4s.api

import com.github.libpq4s.library.IOidBox


trait IPGnotify {
  def getRelname(): String
  def setRelname(value: String): Unit

  def getBePid(): Int
  def setBePid(value: Int): Unit

  def getExtra(): String
  def setExtra(value: String): Unit

  def getNext(): IPGnotify
  def setNext(value: IPGnotify): Unit
}

trait IPGresAttDesc {
  def getName(): String
  def setName(value: String): Unit

  def getTableId(): IOidBox
  def setTableId(value: IOidBox): Unit

  def getColumnId(): Int
  def setColumnId(value: Int): Unit

  def getFormat(): Int
  def setFormat(value: Int): Unit

  def getTypeId(): IOidBox
  def setTypeId(value: IOidBox): Unit

  def getTypeLength(): Int
  def setTypeLength(value: Int): Unit

  def getAttributeTypeModifier(): Int
  def setAttributeTypeModifier(value: Int): Unit
}

trait IPQArgBlock {

  def getLentgh(): Int
  def setLentgh(value: Int): Unit

  def getIsInt(): Int
  def setIsInt(value: Int): Unit

  //def getU: Nothing // FIXME
}

trait IPQconninfoOptionWrapper extends Any {
  //def toConnInfoOption(): Option[IConnInfoOption]
  def forEachConnInfoOption(fn: ConnInfoOption => Unit): Unit
  def free(): Unit
}

/*trait IConnInfoOption {

  /** The name of the option */
  def getKeyword(): String
  //def setKeyword(value: String): Unit

  /** The environment variable to fall back to*/
  def getEnvVar(): String
  //def setEnvVar(value: String): Unit

  /** The compiled in option as a secondary fallback */
  def getCompiled(): String
  //def setCompiled(value: String): Unit

  /** Current value, defined if known, otherwise None */
  def getValue(): Option[String]
  //def setValue(value: String): Unit

  /** Label for field in connect dialog */
  def getLabel(): String
  //def setLabel(value: String): Unit

  /** Indicates how to display this field: '' for normal, 'D' for debug, and '*' for password */
  def getDispChar(): Char
  //def setDispChar(value: String): Unit

  /** Field size in characters for connect dialog */
  def getDispSize(): Int
  //def setDispSize(value: Int): Unit

}*/

trait IPQprintOpt {

  def getHeader(): Char
  def setHeader(value: Char): Unit

  def getAlign(): Char
  def setAlign(value: Char): Unit

  def getStandard(): Char
  def setStandard(value: Char): Unit

  def getHtml3(): Char
  def setHtml3(value: Char): Unit

  def getExpanded(): Char
  def setExpanded(value: Char): Unit

  def getPager(): Char
  def setPager(value: Char): Unit

  def getFieldSep(): String
  def setFieldSep(value: String): Unit

  def getTableOpt(): String
  def setTableOpt(value: String): Unit

  def getCaption(): String
  def setCaption(value: String): Unit

  def getFieldName(): Array[String]
  def setFieldName(value: Array[String]): Unit
}