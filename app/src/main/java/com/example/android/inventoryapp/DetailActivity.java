package com.example.android.inventoryapp;

/**
 * Created by hp on 21-06-2017.
 */

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.InventoryContract.InventoryEntry;

import java.io.FileDescriptor;
import java.io.IOException;


public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = DetailActivity.class.getSimpleName();

    private static final int PICK_IMAGE_REQUEST = 0;
    private static final int MY_PERMISSIONS_REQUEST = 2;

    private ImageView mImageView;
    private EditText mNameEdit;
    private EditText mSupplierEdit;
    private EditText mPriceEdit;
    private EditText mQuantityEdit;

    private Uri myUri;
    private Bitmap mBitmap;
    private Button mDecrement;
    private Button mIncrement;
    private Button mOrder;
    private Button mSave;
    private Button mDelete;
    private Uri mCurrentProductUri;
    private String mUri = "noimages";

    String[] projection = {
            InventoryEntry._ID,
            InventoryEntry.COLUMN_INVENTORY_NAME,
            InventoryEntry.COLUMN_INVENTORY_SUPPLIER,
            InventoryEntry.COLUMN_INVENTORY_PRICE,
            InventoryEntry.COLUMN_INVENTORY_QUANTITY,
            InventoryEntry.COLUMN_INVENTORY_PICTURE
    };

    int quantity = 0;
    private static final int EXISTING_INVENTORY_LOADER = 0;

    private boolean isGalleryPicture = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        mImageView = (ImageView) findViewById(R.id.image_product);
        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImageSelector(view);
            }
        });
        ViewTreeObserver viewTreeObserver = mImageView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mNameEdit = (EditText) findViewById(R.id.name_edit);
        mSupplierEdit = (EditText) findViewById(R.id.supplier_edit);
        mPriceEdit = (EditText) findViewById(R.id.price_edit);
        mQuantityEdit = (EditText) findViewById(R.id.quantity_edit);
        mDecrement = (Button) findViewById(R.id.decrement);
        mDecrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                decrement(view);
            }
        });
        mIncrement = (Button) findViewById(R.id.increment);
        mIncrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                increment(view);
            }
        });

        mOrder = (Button) findViewById(R.id.order);
        mOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = mNameEdit.getText().toString();
                String supplier = mSupplierEdit.getText().toString();
                String price = mPriceEdit.getText().toString();
                String quantity = mQuantityEdit.getText().toString();

                final String email = "Dear " + supplier
                        + "\n Please send some " + name
                        + " at a price of " + price
                        + " and of quantity " + quantity;

                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:")); // only email apps should handle this
                intent.putExtra(Intent.EXTRA_SUBJECT, "Product Order");
                intent.putExtra(Intent.EXTRA_TEXT, email);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });

        mSave = (Button) findViewById(R.id.save);
        mSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveData();
            }
        });
        mDelete = (Button) findViewById(R.id.delete);
        mDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteConfirmationDialog();
            }
        });

        //Check where we came from
        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        if (mCurrentProductUri == null) {
            //User click new product
            setTitle(getString(R.string.add_product_title));
            mOrder.setVisibility(View.GONE);
            mDelete.setVisibility(View.GONE);
        } else {
            //User want to update a specific product
            setTitle(getString(R.string.edit_product_title));
            //Read database for selected Product
            getLoaderManager().initLoader(EXISTING_INVENTORY_LOADER, null, this);
        }
        requestPermissions();
    }

    private void saveData() {
        String name = mNameEdit.getText().toString().trim();
        String supplier = mSupplierEdit.getText().toString().trim();
        String priceString = mPriceEdit.getText().toString().trim();
        String quantityString = mQuantityEdit.getText().toString().trim();
        mUri = String.valueOf(myUri);

        boolean isAllOk = true;
        if (!checkIfValueSet(mNameEdit, "name")) {
            isAllOk = false;
        }
        if (!checkIfValueSet(mPriceEdit, "price")) {
            isAllOk = false;
        }
        if (!checkIfValueSet(mQuantityEdit, "quantity")) {
            isAllOk = false;
        }
        if (!checkIfValueSet(mSupplierEdit, "supplier name")) {
            isAllOk = false;
        }

        if (myUri == null ) {
            isAllOk = false;
            Toast.makeText(this, "Please add Image", Toast.LENGTH_SHORT).show();
        }
        if (!isAllOk) {
            return;
        }



        int price = Integer.parseInt(priceString);
        int quantity = Integer.parseInt(quantityString);

        ContentValues values = new ContentValues();
        values.put(InventoryEntry.COLUMN_INVENTORY_NAME, name);
        values.put(InventoryEntry.COLUMN_INVENTORY_SUPPLIER, supplier);
        values.put(InventoryEntry.COLUMN_INVENTORY_PRICE, price);
        values.put(InventoryEntry.COLUMN_INVENTORY_QUANTITY, quantity);
        values.put(InventoryEntry.COLUMN_INVENTORY_PICTURE, mUri);

        if (mCurrentProductUri == null) {
            Uri insertedRow = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);
            if (insertedRow == null) {
                Toast.makeText(this, R.string.err_inserting_product, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.ok_updated, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, CatalogActivity.class);
                startActivity(intent);
            }
        } else {
            // We are Updating
            int rowUpdated = getContentResolver().update(mCurrentProductUri, values, null, null);

            if (rowUpdated == 0) {
                Toast.makeText(this, R.string.err_inserting_product, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.ok_updated, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, CatalogActivity.class);
                startActivity(intent);
            }
        }
    }

    private boolean checkIfValueSet(EditText text, String description) {
        if (TextUtils.isEmpty(text.getText())) {
            text.setError("Missing product " + description);
            return false;
        } else {
            text.setError(null);
            return true;
        }
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the product.
                deleteData();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void requestPermissions() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            mImageView.setEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    mImageView.setEnabled(true);
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    public void openImageSelector(View view) {
        Intent intent;

        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }

        // Show only images, no videos or anything else


        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    // Save the activity state when it's going to stop.
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("picUri", myUri);
    }

    // Recover the saved state when the activity is recreated.
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        myUri = savedInstanceState.getParcelable("picUri");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code READ_REQUEST_CODE.
        // If the request code seen here doesn't match, it's the response to some other intent,
        // and the below code shouldn't run at all.

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.  Pull that uri using "resultData.getData()"

            if (resultData != null) {
                myUri = resultData.getData();


                mBitmap = getBitmapFromUri(myUri);
                mImageView.setImageBitmap(mBitmap);

                isGalleryPicture = true;
            }
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
            return image;
        } catch (Exception e) {

            return null;
        } finally {
            try {
                if (parcelFileDescriptor != null) {
                    parcelFileDescriptor.close();
                }
            } catch (IOException e) {
                e.printStackTrace();

            }
        }
    }

    private void deleteData() {
        // Only perform the delete if this is an existing product.
        if (mCurrentProductUri != null) {
            // Call the ContentResolver to delete the pet at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentProductUri
            // content URI already identifies the pet that we want.
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.inventory_delete_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.inventory_delete_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }

        // Close the activity
        finish();
    }

    public void increment(View view) {
        if (quantity < 500) {
            quantity = quantity + 1;
        } else {
            quantity = 500;
            Toast toastMessage = Toast.makeText(getApplicationContext(), "You cannot order more", Toast.LENGTH_SHORT);
            toastMessage.show();
        }
        display(quantity);
    }

    public void decrement(View view) {
        if (quantity <= 0) {
            quantity = 0;
            Toast toastMessage = Toast.makeText(getApplicationContext(), "Invalid Input", Toast.LENGTH_SHORT);
            toastMessage.show();
        } else {
            quantity = quantity - 1;
        }
        display(quantity);
    }

    /**
     * This method displays the given quantity value on the screen.
     */
    private void display(int number) {
        TextView quantityTextView = (TextView) findViewById(R.id.quantity_edit);
        quantityTextView.setText("" + number);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                mCurrentProductUri,
                projection,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {

            int i_COLUMN_INVENTORY_NAME = 1;
            int i_COLUMN_INVENTORY_QUANTITY = 2;
            int i_COLUMN_INVENTORY_PRICE = 3;
            int i_COLUMN_INVENTORY_SUPPLIER = 4;
            int i_COLUMN_INVENTORY_PICTURE = 5;

            // Extract values from current cursor
            String name = cursor.getString(i_COLUMN_INVENTORY_NAME);
            int quantity = cursor.getInt(i_COLUMN_INVENTORY_QUANTITY);
            int price = cursor.getInt(i_COLUMN_INVENTORY_PRICE);
            String supplier = cursor.getString(i_COLUMN_INVENTORY_SUPPLIER );
            mUri = cursor.getString(i_COLUMN_INVENTORY_PICTURE);
            mNameEdit.setText(name);
            mSupplierEdit.setText(supplier);
            mPriceEdit.setText(String.valueOf(price));
            mQuantityEdit.setText(String.valueOf(quantity));
            mImageView.setImageURI(Uri.parse(mUri));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mNameEdit.setText("");
        mPriceEdit.setText("");
        mQuantityEdit.setText("");
        mSupplierEdit.setText("");
        mImageView.setImageURI(myUri);

    }
}
