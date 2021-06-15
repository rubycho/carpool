package com.example.carpool.ds

import android.os.Parcel
import android.os.Parcelable


class Request() : Parcelable {
    var chName = ""
    var reqId = 0L
    var category = ""

    constructor(parcel: Parcel) : this() {
        chName = parcel.readString().toString()
        reqId = parcel.readLong()
        category = parcel.readString().toString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(chName)
        parcel.writeLong(reqId)
        parcel.writeString(category)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Request> {
        override fun createFromParcel(parcel: Parcel): Request {
            return Request(parcel)
        }

        override fun newArray(size: Int): Array<Request?> {
            return arrayOfNulls(size)
        }
    }

}

class RequestDismiss() : Parcelable {
    var chName = ""
    var reqId = 0L

    constructor(parcel: Parcel) : this() {
        chName = parcel.readString().toString()
        reqId = parcel.readLong()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(chName)
        parcel.writeLong(reqId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RequestDismiss> {
        override fun createFromParcel(parcel: Parcel): RequestDismiss {
            return RequestDismiss(parcel)
        }

        override fun newArray(size: Int): Array<RequestDismiss?> {
            return arrayOfNulls(size)
        }
    }
}
