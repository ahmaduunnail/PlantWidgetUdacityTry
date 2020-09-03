package com.example.android.mygarden;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.style.IconMarginSpan;

import com.example.android.mygarden.provider.PlantContract;
import com.example.android.mygarden.utils.PlantUtils;

import static com.example.android.mygarden.provider.PlantContract.BASE_CONTENT_URI;
import static com.example.android.mygarden.provider.PlantContract.INVALID_PLANT_ID;
import static com.example.android.mygarden.provider.PlantContract.PATH_PLANTS;


public class PlantWateringService extends IntentService {

    public static final String ACTION_WATER_PLANT = "com.example.android.mygarden.action.water_plant";
    public static final String ACTION_UPDATE_PLANT_WIDGETS = "com.example.android.mygarden.action.update_plant_widgets";
    public static final String EXTRA_PLANT_ID = "com.example.android.mygarden.extra.PLANT_ID";

    public PlantWateringService() {
        super("PlantWateringService");
    }

    public static void startActionWaterPlants(Context context, long plantId) {
        Intent intent = new Intent(context, PlantWateringService.class);
        intent.setAction(ACTION_WATER_PLANT);
        intent.putExtra(EXTRA_PLANT_ID, plantId);
        context.startService(intent);
    }

    public static void startActionUpdatePlantWidgets(Context context) {
        Intent intent = new Intent(context, PlantWateringService.class);
        intent.setAction(ACTION_UPDATE_PLANT_WIDGETS);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_WATER_PLANT.equals(action)) {
                final long plantId = intent.getLongExtra(EXTRA_PLANT_ID,
                        PlantContract.INVALID_PLANT_ID);
                handleActionWaterPlants(plantId);
            } else if (ACTION_UPDATE_PLANT_WIDGETS.equals(action)) {
                handleActionUpdateWaterPlantWidgets();
            }
        }
    }

    private void handleActionUpdateWaterPlantWidgets() {
        Uri PLANT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLANTS).build();
        Cursor cursor = getContentResolver().query(
                PLANT_URI,
                null,
                null,
                null,
                PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME
        );
        int image = R.drawable.grass;
        long plantID = INVALID_PLANT_ID;
        boolean waters = false;
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            int id = cursor.getColumnIndex(PlantContract.PlantEntry._ID);
            int createTime = cursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_CREATION_TIME);
            int waterTime = cursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME);
            int plantType = cursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_PLANT_TYPE);
            long timeNow = System.currentTimeMillis();
            plantID = cursor.getLong(id);
            long create = cursor.getLong(createTime);
            long water = cursor.getLong(waterTime);
            int type = cursor.getInt(plantType);
            cursor.close();
            waters = (timeNow - water) > PlantUtils.MIN_AGE_BETWEEN_WATER && (timeNow - water) < PlantUtils.MAX_AGE_WITHOUT_WATER;
            image = PlantUtils.getPlantImageRes(this, timeNow - create, timeNow - water, type);
        }
        AppWidgetManager appwidget = AppWidgetManager.getInstance(this);
        int[] appwidgetIds = appwidget.getAppWidgetIds(new ComponentName(this, PlantWidgetProvider.class));
        PlantWidgetProvider.updatePlantsWidgets(this, appwidget, image, plantID, waters, appwidgetIds);
    }

    private void handleActionWaterPlants(long plantId) {
        Uri SINGLE_PLANTS_URI = ContentUris.withAppendedId(BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLANTS).build(), plantId);
        ContentValues contentValues = new ContentValues();
        long timeNow = System.currentTimeMillis();
        contentValues.put(PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME, timeNow);
        // Update only plants that are still alive
        getContentResolver().update(
                SINGLE_PLANTS_URI,
                contentValues,
                PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME + ">?",
                new String[]{String.valueOf(timeNow - PlantUtils.MAX_AGE_WITHOUT_WATER)});
        startActionUpdatePlantWidgets(this);
    }
}
