package com.example.android.inventoryapp;

/**
 * Created by hp on 28-06-2017.
 */

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import static com.example.android.inventoryapp.data.InventoryContract.InventoryEntry;


public class InventoryCursorAdapter extends CursorAdapter {

    private static final String TAG = InventoryCursorAdapter.class.getSimpleName();


    protected InventoryCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);

    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        TextView product_name = (TextView) view.findViewById(R.id.inventory_name);
        TextView product_quantity = (TextView) view.findViewById(R.id.inventory_quantity);
        TextView product_price = (TextView) view.findViewById(R.id.inventory_price);
        ImageView product_add_btn = (ImageView) view.findViewById(R.id.sale_product);


        int nameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_NAME);
        int quantityColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_QUANTITY);
        int priceColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_PRICE);


        int id = cursor.getInt(cursor.getColumnIndex(InventoryEntry._ID));
        final String productName = cursor.getString(nameColumnIndex);
        final int quantity = cursor.getInt(quantityColumnIndex);
        String productPrice = "Price INR: " + cursor.getString(priceColumnIndex);

        String productQuantity = String.valueOf(quantity) + " Inventory";

        final Uri currentProductUri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, id);



        product_name.setText(productName);
        product_quantity.setText(productQuantity);
        product_price.setText(productPrice);


        product_add_btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                ContentResolver resolver = view.getContext().getContentResolver();
                ContentValues values = new ContentValues();
                if (quantity > 0) {
                    int qq = quantity;
                    //int yy = products_sold;

                    values.put(InventoryEntry.COLUMN_INVENTORY_QUANTITY, --qq);
                    resolver.update(
                            currentProductUri,
                            values,
                            null,
                            null
                    );
                    context.getContentResolver().notifyChange(currentProductUri, null);
                } else {
                    Toast.makeText(context, "Item out of stock", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}
