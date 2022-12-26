package com.envious.searchphoto.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.envious.searchphoto.R
import com.envious.searchphoto.base.BaseFragment
import com.envious.searchphoto.databinding.MainFragmentBinding
import com.envious.searchphoto.ui.adapter.ItemAdapter
import com.envious.searchphoto.ui.searchresult.SearchResultFragment
import com.envious.searchphoto.util.EndlessRecyclerViewScrollListener
import com.envious.searchphoto.util.Intent
import com.envious.searchphoto.util.State
import com.envious.searchphoto.util.ViewState

class MainFragment : BaseFragment<Intent,
    State>() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private var _binding: MainFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ItemAdapter

    private lateinit var scrollListener: EndlessRecyclerViewScrollListener
    private var currentPage = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = MainFragmentBinding.inflate(layoutInflater)
        (activity as AppCompatActivity?)!!.supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.state.observe(viewLifecycleOwner) {
            invalidate(it)
        }
        setupRecyclerView()
        viewModel.onIntentReceived(Intent.GetCollection)
        setUpButtonSearch(view)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        with(binding) {
            recyclerview.setHasFixedSize(true)
            val gridLayoutManager = GridLayoutManager(requireContext(), 2)
            recyclerview.layoutManager = gridLayoutManager
            recyclerview.itemAnimator = null
            adapter = ItemAdapter(requireContext(), false)
            adapter.setHasStableIds(true)
            recyclerview.adapter = adapter
            scrollListener = object : EndlessRecyclerViewScrollListener(gridLayoutManager) {
                override fun onLoadMore(
                    page: Int,
                    totalItemsCount: Int,
                    view: RecyclerView?,
                ) {
                    currentPage = page + 1
                    viewModel.onIntentReceived(Intent.LoadNextCollection(currentPage))
                }
            }
            recyclerview.addOnScrollListener(scrollListener)
        }
    }

    override val layoutResourceId: Int
        get() = R.layout.main_fragment

    override fun invalidate(state: State) {
        with(binding) {
            pgProgressList.visibility = if (state.showLoading) View.VISIBLE else View.GONE
        }

        when (state.viewState) {
            ViewState.EmptyListFirstInit -> {
                with(binding) {
                    errorView.visibility = View.VISIBLE
                    errorView.run {
                        setUpErrorView(
                            title = resources.getString(R.string.empty_state_title),
                            message = resources.getString(R.string.empty_state_message)
                        )
                        binding.buttonRetry.setOnClickListener {
                            currentPage = 1
                            viewModel.onIntentReceived(Intent.GetCollection)
                        }
                    }
                    adapter.setList(emptyList())
                    recyclerview.visibility = View.GONE
                }
            }
            ViewState.ErrorFirstInit -> {
                with(binding) {
                    errorView.visibility = View.VISIBLE
                    errorView.run {
                        setUpErrorView()
                        binding.buttonRetry.setOnClickListener {
                            currentPage = 1
                            viewModel.onIntentReceived(Intent.GetCollection)
                        }
                    }
                    adapter.setList(emptyList())
                    recyclerview.visibility = View.GONE
                }
            }
            ViewState.ErrorLoadMore -> {
                with(binding) {
                    recyclerview.visibility = View.VISIBLE
                }
            }
            ViewState.Idle -> {}
            ViewState.SuccessFirstInit -> {
                with(binding) {
                    recyclerview.visibility = View.VISIBLE
                    adapter.setList(state.listPhoto)
                    errorView.visibility = View.GONE
                }
            }
            ViewState.SuccessLoadMore -> {
                with(binding) {
                    recyclerview.visibility = View.VISIBLE
                    adapter.addData(state.listPhoto)
                    errorView.visibility = View.GONE
                }
            }
            ViewState.BackToSearchResult -> {}
        }
    }

    private fun setUpButtonSearch(view: View) {
        with(binding) {
            buttonSearch.setOnClickListener {
                if (!textSearch.text.isNullOrEmpty()) {
                    val bundle = Bundle()
                    bundle.putString(SearchResultFragment.EXTRA_QUERY, textSearch.text.toString())
                    Navigation.findNavController(view).navigate(R.id.action_mainFragment_to_searchResultFragment, bundle)
                }
            }
        }
    }
}
