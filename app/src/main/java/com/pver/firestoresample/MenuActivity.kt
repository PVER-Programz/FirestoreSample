package com.pver.firestoresample

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MenuActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var restaurantSpinner: Spinner
    private lateinit var menuRecyclerView: RecyclerView
    private lateinit var menuAdapter: MenuAdapter

    private var currentRestaurant: String? = null
    private val cartItems = mutableMapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_menu)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            val email = "pramodviknesh.e2024@vitstudent.ac.in"
            val password = "12345678"       // Hardcoded password

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    Toast.makeText(this, "Logged in as ${email}", Toast.LENGTH_SHORT).show()
                    // Continue with loading menu data
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Login failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Already logged in as ${currentUser?.email}", Toast.LENGTH_SHORT).show()
            // Continue with loading menu data
        }

        restaurantSpinner = findViewById(R.id.restaurantSpinner)
        val button = findViewById<Button>(R.id.button)
        menuRecyclerView = findViewById(R.id.menuRecyclerView)

        menuRecyclerView.layoutManager = LinearLayoutManager(this)
        menuAdapter = MenuAdapter { dishName, increment ->
            updateCart(dishName, increment)
        }
        menuRecyclerView.adapter = menuAdapter

        fetchRestaurants()

        button.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            startActivity(intent)
        }
    }

    private fun fetchRestaurants() {
        db.collection("Menu").get()
            .addOnSuccessListener { result ->
                val restaurantList = result.documents.map { it.id }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, restaurantList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                restaurantSpinner.adapter = adapter

                restaurantSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        val selected = restaurantList[position]
                        if (selected != currentRestaurant) {
                            currentRestaurant = selected
                            cartItems.clear()
                            loadMenuForRestaurant(selected)
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
            }
    }

    private fun loadMenuForRestaurant(restaurant: String) {
        db.collection("Menu").document(restaurant).get()
            .addOnSuccessListener { document ->
                val menuMap = document.data?.mapValues { (it.value as? Number)?.toDouble() ?: 0.0 } ?: emptyMap()
                menuAdapter.setMenu(menuMap)
            }
    }

    private fun updateCart(dishName: String, increment: Boolean) {
        if (currentRestaurant == null) return
        val currentQty = cartItems[dishName] ?: 0
        if (increment) {
            cartItems[dishName] = currentQty + 1
        } else {
            if (currentQty > 0) cartItems[dishName] = currentQty - 1
            if (cartItems[dishName] == 0) cartItems.remove(dishName)
        }
        syncCartToFirestore()
    }

    private fun syncCartToFirestore() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val cartData = mapOf(
            "shopname" to currentRestaurant,
            "items" to cartItems
        )

        db.collection("Users").document(user.email ?: return)
            .update("cart", cartData)
            .addOnSuccessListener {
                Toast.makeText(this, "Cart updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update cart", Toast.LENGTH_SHORT).show()
            }
    }
}

class MenuAdapter(
    private val onCartUpdate: (String, Boolean) -> Unit
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    private var menuItems: Map<String, Double> = emptyMap()

    fun setMenu(menu: Map<String, Double>) {
        this.menuItems = menu
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_dish, parent, false)
        return MenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val dishName = menuItems.keys.elementAt(position)
        val price = menuItems[dishName] ?: 0.0
        holder.bind(dishName, price)
    }

    override fun getItemCount() = menuItems.size

    inner class MenuViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val dishNameText: TextView = view.findViewById(R.id.dishName)
        private val priceText: TextView = view.findViewById(R.id.dishPrice)
        private val plusBtn: Button = view.findViewById(R.id.plusBtn)
        private val minusBtn: Button = view.findViewById(R.id.minusBtn)

        fun bind(dishName: String, price: Double) {
            dishNameText.text = dishName
            priceText.text = "₹$price"
            plusBtn.setOnClickListener { onCartUpdate(dishName, true) }
            minusBtn.setOnClickListener { onCartUpdate(dishName, false) }
        }
    }
}
