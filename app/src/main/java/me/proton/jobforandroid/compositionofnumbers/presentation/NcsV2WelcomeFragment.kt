package me.proton.jobforandroid.compositionofnumbers.presentation

import android.app.Dialog
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.LeadingMarginSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import me.proton.jobforandroid.compositionofnumbers.R
import me.proton.jobforandroid.compositionofnumbers.databinding.NcsV2WelcomeBinding

class NcsV2WelcomeFragment : Fragment() {
    private var _binding: NcsV2WelcomeBinding? = null
    private val binding get() = _binding!!
    private var sounds: UiSoundPool? = null
    private var documentDialog: Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = NcsV2WelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sounds = UiSoundPool(requireContext(), R.raw.key_press)
        binding.ncsV2ChooseLessonButton.installButtonPressAnimation()
        binding.ncsV2ChooseLessonButton.setOnClickListener {
            val navController = findNavController()
            if (navController.currentDestination?.id == R.id.ncsV2WelcomeFragment) {
                sounds?.play(R.raw.key_press)
                navController.navigate(R.id.action_ncsV2WelcomeFragment_to_ncsV2LessonSelectionFragment)
            }
        }
        binding.ncsV2DocumentsButton.setOnClickListener { showDocuments() }
    }

    private fun showDocuments() {
        if (documentDialog?.isShowing == true) return
        val context = requireContext()
        val dialogView = layoutInflater.inflate(R.layout.ncs_v2_documents_dialog, null)
        val privacyTab = dialogView.findViewById<TextView>(R.id.ncs_v2_privacy_tab)
        val termsTab = dialogView.findViewById<TextView>(R.id.ncs_v2_terms_tab)
        val bodyView = dialogView.findViewById<TextView>(R.id.ncs_v2_document_body)
        val scrollView = dialogView.findViewById<ScrollView>(R.id.ncs_v2_document_scroll)
        fun selectDocument(bodyRes: Int) {
            val body = getString(bodyRes)
            val formatted = SpannableString(body)
            Regex("(?m)^\\d+\\. [^\\n]+|^(?:Разработчик|Приложение|Электронная почта|Developer|Application|Email):[^\\n]*")
                .findAll(body).forEach { match ->
                    formatted.setSpan(
                        StyleSpan(Typeface.BOLD), match.range.first, match.range.last + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            val indent = (12 * context.resources.displayMetrics.density).toInt()
            Regex("(?m)^(?!\\d+\\.|ПОЛИТИКА|УСЛОВИЯ|NUMCOMPSIM|Дата вступления|Effective date|Разработчик:|Приложение:|Электронная почта:|Developer:|Application:|Email:|https?://|$)[^\\n]+")
                .findAll(body).forEach { match ->
                    formatted.setSpan(
                        LeadingMarginSpan.Standard(indent, 0), match.range.first,
                        match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            bodyView.text = formatted
            val privacySelected = bodyRes == R.string.privacy_policy_full_text
            privacyTab.setBackgroundResource(if (privacySelected) R.drawable.ncs_v2_selected_outline else android.R.color.transparent)
            termsTab.setBackgroundResource(if (privacySelected) android.R.color.transparent else R.drawable.ncs_v2_selected_outline)
            privacyTab.setTextColor(ContextCompat.getColor(context, if (privacySelected) R.color.ncs_v2_primary else R.color.ncs_v2_muted))
            termsTab.setTextColor(ContextCompat.getColor(context, if (privacySelected) R.color.ncs_v2_muted else R.color.ncs_v2_primary))
            scrollView.scrollTo(0, 0)
        }
        privacyTab.setOnClickListener { selectDocument(R.string.privacy_policy_full_text) }
        termsTab.setOnClickListener { selectDocument(R.string.terms_and_conditions_full_text) }
        selectDocument(R.string.privacy_policy_full_text)
        val dialog = Dialog(context).apply {
            setContentView(dialogView)
            window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            setOnDismissListener { documentDialog = null }
        }
        dialogView.findViewById<TextView>(R.id.ncs_v2_document_close_button)
            .setOnClickListener { dialog.dismiss() }
        documentDialog = dialog
        dialog.show()
        dialog.window?.setLayout(
            (binding.root.width * 0.92f).toInt(),
            (binding.root.height * 0.85f).toInt()
        )
    }

    override fun onDestroyView() {
        documentDialog?.dismiss()
        documentDialog = null
        binding.ncsV2ChooseLessonButton.cancelButtonPressAnimation()
        sounds?.release()
        sounds = null
        super.onDestroyView()
        _binding = null
    }
}
