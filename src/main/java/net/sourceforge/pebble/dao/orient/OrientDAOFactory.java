/*
 * Copyright (c) 2003-2011, Simon Brown
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *
 *   - Neither the name of Pebble nor the names of its contributors may
 *     be used to endorse or promote products derived from this software
 *     without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package net.sourceforge.pebble.dao.orient;

import com.orientechnologies.orient.core.db.object.ODatabaseObjectTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import net.sourceforge.pebble.dao.*;
import net.sourceforge.pebble.dao.file.FileCategoryDAO;
import net.sourceforge.pebble.dao.file.FileRefererFilterDAO;
import net.sourceforge.pebble.dao.file.FileStaticPageDAO;
import net.sourceforge.pebble.dao.orient.model.OrientBlogEntry;
import net.sourceforge.pebble.domain.Blog;

import java.io.File;

/**
 * Represents a strategy used to load and store blog entries
 * in OrientDB
 *
 * @author Simon Brown
 */
public class OrientDAOFactory extends DAOFactory {

  public static final String ORIENT_STORAGE_TYPE = "orient";

  private final BlogEntryDAO blogEntryDAO;
  private final StaticPageDAO staticPageDAO;
  private final CategoryDAO categoryDAO;
  private final RefererFilterDAO refererFilterDAO;
  private final String databasePath;


  public OrientDAOFactory(File pebbleHome) {
    this.databasePath = "local:" + pebbleHome.getPath() + File.separator + "orientdb" + File.separator;
    this.blogEntryDAO = new OrientBlogEntryDAO(this);
    this.staticPageDAO = new FileStaticPageDAO();
    this.categoryDAO = new FileCategoryDAO();
    this.refererFilterDAO = new FileRefererFilterDAO();
  }

  @Override
  public void init(Blog blog) {
    ODatabaseObjectTx db = getDb(blog);
    try {
      if (!db.exists()) {
        db.create();
      }
      createIndex(db, OrientBlogEntry.class, "id", OType.STRING);
    } finally {
      db.close();
    }
  }

  private void createIndex(ODatabaseObjectTx db, Class clazz, String property, OType type) {
    OSchema schema = db.getMetadata().getSchema();
    OClass oclass;
    if (!schema.existsClass(clazz.getSimpleName())) {
        oclass = schema.createClass(clazz);
    } else {
      oclass = schema.getClass(clazz);
    }
    if (!oclass.existsProperty(property)) {
      oclass.createProperty(property, type);
    }
    String indexName = clazz.getSimpleName() + "." + property;
    if (db.getMetadata().getIndexManager().getIndex(indexName) == null) {
      db.command(new OCommandSQL("create index " + indexName + " unique")).execute();
    }
  }

  public ODatabaseObjectTx getDb(Blog blog) {
    ODatabaseObjectTx db = new ODatabaseObjectTx(databasePath + blog.getId());
    db.getEntityManager().registerEntityClasses("net.sourceforge.pebble.dao.orient.model");
    if (db.exists()) {
      return db.open("admin", "admin");
    }
    return db;
  }

  /**
   * Gets a DAO instance responsible for the dao of blog entries.
   *
   * @return a BlogEntryDAO instance
   */
  public BlogEntryDAO getBlogEntryDAO() {
    return this.blogEntryDAO;
  }

  /**
   * Gets a DAO instance responsible for the dao of static pages.
   *
   * @return a StaticPageDAO instance
   */
  public StaticPageDAO getStaticPageDAO() {
    return this.staticPageDAO;
  }

  /**
   * Gets a DAO instance responsible for the dao of categories.
   *
   * @return a CategoryDAO instance
   */
  public CategoryDAO getCategoryDAO() {
    return this.categoryDAO;
  }

  /**
   * Gets a DAO instance responsible for the dao of referer filters.
   *
   * @return a RefererFilterDAO instance
   */
  public RefererFilterDAO getRefererFilterDAO() {
    return this.refererFilterDAO;
  }

}
