/*
 * The MIT License (MIT)
 *
 * Copyright (C) 2012 47 Degrees, LLC http://47deg.com hello@47deg.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 */

package com.fortysevendeg.mvessel.sample.scala.ui

import android.app.{Activity, LoaderManager}
import android.content.{Context, Loader}
import android.database.Cursor
import android.os.Bundle
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.{CursorAdapter, ListView, TextView}
import com.fortysevendeg.mvessel._
import com.fortysevendeg.mvessel.sample.scala.db.{ContactsCursorLoader, ContactsOpenHelper}
import com.fortysevendeg.mvessel.sample.scala.{R, TR, TypedFindView}

class MainActivity
  extends Activity with LoaderManager.LoaderCallbacks[Cursor]
  with TypedFindView {

  private[this] lazy val contactsOpenHelper: ContactsOpenHelper = new ContactsOpenHelper(this)
  private[this] lazy val listView: ListView = findView(TR.list_view)

  protected override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.sample_main)
    (contactsOpenHelper, listView)
    getLoaderManager.restartLoader(0, javaNull, this)
  }

  protected override def onDestroy() = {
    contactsOpenHelper.close()
    super.onDestroy()
  }

  def onCreateLoader(id: Int, args: Bundle): Loader[Cursor] = {
    new ContactsCursorLoader(this, contactsOpenHelper.database)
  }

  def onLoadFinished(loader: Loader[Cursor], data: Cursor) = {
    val adapter: ContactsAdapter = new ContactsAdapter(this, data)
    listView.setAdapter(adapter)
  }

  def onLoaderReset(loader: Loader[Cursor]) = {}
}

class ContactsAdapter(context: Context, c: Cursor)
  extends CursorAdapter(context, c, false) {

  val nameColumn: Int = c.getColumnIndex(ContactsOpenHelper.C_NAME)
  val ageColumn: Int = c.getColumnIndex(ContactsOpenHelper.C_AGE)

  def newView(context: Context, cursor: Cursor, parent: ViewGroup): View =
    parent.getContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) match {
      case inflater: LayoutInflater =>
        inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
      case _ =>
        throw new IllegalStateException("Can't get a layout reference")
    }

  def bindView(view: View, context: Context, cursor: Cursor) {
    val textView: TextView = view.findViewById(android.R.id.text1).asInstanceOf[TextView]
    textView.setText(s"${cursor.getString(nameColumn)} (${cursor.getInt(ageColumn)})")
  }
}