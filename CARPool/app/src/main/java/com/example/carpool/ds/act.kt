package com.example.carpool.ds

import android.os.Parcel
import android.os.Parcelable


class Inform() : Parcelable {
    var chName = ""
    var actId = 0L
    var category = ""
    var start = 0L
    var end = 0L

    constructor(parcel: Parcel) : this() {
        chName = parcel.readString().toString()
        actId = parcel.readLong()
        category = parcel.readString().toString()
        start = parcel.readLong()
        end = parcel.readLong()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(chName)
        parcel.writeLong(actId)
        parcel.writeString(category)
        parcel.writeLong(start)
        parcel.writeLong(end)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Inform> {
        override fun createFromParcel(parcel: Parcel): Inform {
            return Inform(parcel)
        }

        override fun newArray(size: Int): Array<Inform?> {
            return arrayOfNulls(size)
        }
    }
}
