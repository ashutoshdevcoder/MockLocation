package com.ashdev.mocklocation.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.ashdev.mocklocation.R
import com.ashdev.mocklocation.databinding.DialogInputBinding
import com.ashdev.mocklocation.helper.getLocationFromAddress
import com.ashdev.mocklocation.helper.init
import com.ashdev.mocklocation.helper.showToast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InputDialogFragment : DialogFragment() {
    private lateinit var binding: DialogInputBinding
    private var mContext: Context? = null
    private lateinit var dialog:Dialog
    override fun getTheme(): Int {
        return R.style.AppDialogTheme
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialog=(super.onCreateDialog(savedInstanceState)).init(true)
        return dialog
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogInputBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnApply.setOnClickListener {
            setAddress()
        }
        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun setAddress() {
        val address=binding.etInput.text.toString()
        if(address.isEmpty())
            return
       val latLng= context?.getLocationFromAddress(address)
        if(latLng!=null && latLng.latitude >0 && latLng.longitude > 0)
        {
            (activity as? MainActivity)?.applyLatLng(latLng,address)
            dialog.dismiss()
        }
        else
            context?.showToast(getString(R.string.unable_to_find_lat_lng))
    }
    companion object {
        @JvmStatic
        fun newInstance() =
            InputDialogFragment()
            }

}