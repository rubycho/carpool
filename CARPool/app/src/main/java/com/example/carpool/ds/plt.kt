package com.example.carpool.ds

import android.os.Parcel
import android.os.Parcelable


class Trigger() : Parcelable {
    var actId = 0L
    var invokeAt = ""

    constructor(parcel: Parcel) : this() {
        actId = parcel.readLong()
        invokeAt = parcel.readString().toString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(actId)
        parcel.writeString(invokeAt)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Trigger> {
        override fun createFromParcel(parcel: Parcel): Trigger {
            return Trigger(parcel)
        }

        override fun newArray(size: Int): Array<Trigger?> {
            return arrayOfNulls(size)
        }
    }

}
