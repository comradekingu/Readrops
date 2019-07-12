package com.readrops.app.views;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.readrops.app.R;
import com.readrops.app.database.entities.Account;
import com.readrops.app.databinding.AccountTypeItemBinding;

import java.util.List;

public class AccountTypeListAdapter extends RecyclerView.Adapter<AccountTypeListAdapter.AccountTypeViewHolder> {

    private List<Account.AccountType> accountTypes;
    private OnItemClickListener listener;

    public AccountTypeListAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public AccountTypeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AccountTypeItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()),
                R.layout.account_type_item, parent, false);

        return new AccountTypeViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountTypeViewHolder holder, int position) {
        Account.AccountType accountType = accountTypes.get(position);

        holder.binding.accountTypeName.setText(accountType.getName());
        holder.binding.accountTypeLogo.setImageResource(accountType.getIconRes());

        holder.binding.getRoot().setOnClickListener(v -> listener.onItemClick(accountType));
    }

    @Override
    public int getItemCount() {
        return accountTypes.size();
    }

    public void setAccountTypes(List<Account.AccountType> accountTypes) {
        this.accountTypes = accountTypes;
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onItemClick(Account.AccountType accountType);
    }

    public class AccountTypeViewHolder extends RecyclerView.ViewHolder {

        private AccountTypeItemBinding binding;

        public AccountTypeViewHolder(AccountTypeItemBinding binding) {
            super(binding.getRoot());

            this.binding = binding;
        }
    }
}