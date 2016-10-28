package com.devoxx.data;

import com.devoxx.utils.Logger;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import android.content.Context;

import io.realm.DynamicRealm;
import io.realm.DynamicRealmObject;
import io.realm.FieldAttribute;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmMigration;
import io.realm.RealmResults;
import io.realm.RealmSchema;

@EBean(scope = EBean.Scope.Singleton)
public class RealmProvider {

	private static final String DATABASE_NAME = "devoxx_db";

	@RootContext
	Context context;

	private boolean inited = false;

	public void init() {
		final RealmConfiguration configuration =
				new RealmConfiguration.Builder(context)
						.name(DATABASE_NAME)
						.schemaVersion(3)
						.migration(new SchemaMigration())
						.build();
		Realm.setDefaultConfiguration(configuration);

		inited = true;
	}

	public Realm getRealm() {
		if (!inited) {
			init();
		}

		return Realm.getDefaultInstance();
	}

	private static class SchemaMigration implements RealmMigration {

		@Override public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
			Logger.l("Migration needed, old: " + oldVersion + ", new: " + newVersion);

			final RealmSchema schema = realm.getSchema();

			if (oldVersion == 1) {
				if (!schema.get("RealmNotification").hasField("talkEndTime")) {
					schema.get("RealmNotification")
							.addField("talkEndTime", long.class);
				}

				if (!schema.contains("RealmFavouriteTalk")) {
					schema.create("RealmFavouriteTalk")
							.addField("talkId", String.class, FieldAttribute.PRIMARY_KEY);
					final RealmResults<DynamicRealmObject> results = realm.allObjects("RealmNotification");
					for (DynamicRealmObject result : results) {
						final DynamicRealmObject favTalk = realm.createObject("RealmFavouriteTalk");
						favTalk.setString("talkId", result.getString("slotId"));
					}
				}

				oldVersion++;
			}

			if (oldVersion == 2) {
				migrateFrom2to3(schema);
				oldVersion++;
			}
		}

		private void migrateFrom2to3(RealmSchema schema) {
			if (!schema.contains("RealmScheduleCustomFilter")) {
				schema.create("RealmScheduleCustomFilter")
						.addField("key", String.class, FieldAttribute.PRIMARY_KEY)
						.addField("label", String.class)
						.addField("isActive", Boolean.class, FieldAttribute.REQUIRED);
			}
		}
	}
}
