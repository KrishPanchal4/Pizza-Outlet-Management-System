import java.util.*;
import java.sql.*;

class Order {
    String customerName;
    String customerId;
    int orderId;
    List<String> orderedItems;
    String orderStatus;
    float totalAmount;

    public Order(String customerName, String customerId, int orderId, List<String> orderedItems, float totalAmount) {
        this.customerName = customerName;
        this.customerId = customerId;
        this.orderId = orderId;
        this.orderedItems = orderedItems;
        this.totalAmount = totalAmount;
        this.orderStatus = "Pending";
    }

    public void displayOrder() {
        System.out.println("------------------------");
        System.out.println("Order Details");
        System.out.println("Order ID : " + orderId);
        System.out.println("Customer ID : " + customerId);
        System.out.println("Customer Name: " + customerName);
        System.out.println("Ordered Items:");
        for (String item : orderedItems) {
            System.out.println("- " + item);
        }
        System.out.println("Total Amount: Rs." + totalAmount);
        System.out.println("Order Status: " + orderStatus);
        System.out.println("------------------------");
    }
}

class Customer {
    String name;
    String Id;
    DBconnection db = new DBconnection();
    Connection con = db.getConnection();
    Scanner sc = new Scanner(System.in);
    Queue<Order> orderQueue;

    Customer() {
        orderQueue = new LinkedList<>();
    }

    public void viewMenu() {
        System.out.println();
        System.out.println("----------Menu----------");
        String sql = "Select * from menu";
        try {
            PreparedStatement pst = con.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                System.out.println(rs.getInt(1) + ". " + rs.getString(2) + " -  Rs." + rs.getFloat(3));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println();
    }

    public void placeOrder() {
        System.out.print("Enter Your Name : ");
        name = sc.next();
        System.out.print("Enter Your ID : ");
        Id = sc.next();
        viewMenu();
        List<String> orderedItems = new ArrayList<>();
        float totalAmount = 0;

        while (true) {
            try {
                System.out.print("Enter Pizza Number or 0 to Finish : ");
                int itemId = sc.nextInt();
                if (itemId == 0) {
                    break;
                }
                String itemName = getPizzaName(itemId);
                totalAmount += getPizzaPrice(itemId);
                if (itemName != null) {
                    orderedItems.add(itemName);
                    System.out.println(itemName + " has been added to your order.");
                } else {
                    System.out.println("Invalid item ID. Please try again.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a valid integer for Pizza Number.");
                sc.next();
            }
        }

        if (!orderedItems.isEmpty()) {
            System.out.println("To finalize your order Please proceed to Payment");
            paymentMethods();
            if (isPaymentDone) {
                Order order = new Order(name, Id, generateOrderId(), orderedItems, totalAmount);
                orderQueue.add(order);
                insertOrderIntoDatabase(order);
                order.displayOrder();
                System.out.println("Order Successfully added");
            } else {
                System.out.println("Order Failed");
            }
        } else {
            System.out.println("No Items Added to Your Order");
        }
    }

    private void insertOrderIntoDatabase(Order order) {
        try {
            String sql = "Insert Into OrderHistory (orderId , customerId,customerName,orderedItems, totalAmount) Values (?,?, ?, ?, ?)";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, order.orderId);
            pst.setString(2, order.customerId);
            pst.setString(3, order.customerName);
            pst.setString(4, String.join(", ", order.orderedItems));
            pst.setFloat(5, order.totalAmount);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int generateOrderId() {
        Random random = new Random();
        int minOrderId = 1000;
        int maxOrderId = 9999;
        return random.nextInt((maxOrderId - minOrderId) + 1) + minOrderId;
    }

    public void cancelOrder() {
        viewQueue();
        try {
            System.out.print("Enter Order Id to Cancel Order : ");
            int oid = sc.nextInt();
            Order orderToRemove = null;

            if (!orderQueue.isEmpty()) {
                for (Order order : orderQueue) {
                    if (order.orderId == oid) {
                        orderToRemove = order;
                        break;
                    }
                }
                if (orderToRemove != null) {
                    orderQueue.remove(orderToRemove);
                    removeOrderFromDatabase(orderToRemove);
                    System.out.println("Order Cancelled");
                } else {
                    System.out.println("Order not found. Please enter a valid Order Id.");
                }
            } else {
                System.out.println("No orders to cancel");
            }
        } catch (InputMismatchException e) {
            System.out.println("Invalid input. Please enter a valid integer for Order Id.");
            sc.next();
        }
    }

    private void removeOrderFromDatabase(Order order) {
        String sql = "Delete From OrderHistory where orderId = ?";
        try {
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, order.orderId);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private void viewQueue() {
        if (!orderQueue.isEmpty()) {
            for (Order order : orderQueue) {
                order.displayOrder();
            }
        } else if (orderQueue.isEmpty()) {
            System.out.println("Order Queue is Empty");
        }
    }

    public void viewOrderHistory() {
        try {
            int cnt = 0;
            String sql = "Select * from OrderHistory where customerId = ?";
            PreparedStatement pst = con.prepareStatement(sql);
            System.out.print("Enter Customer Id to View History : ");
            String vid = sc.next();
            pst.setString(1, vid);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                System.out.println("***************************");
                System.out.println("Order Id : " + rs.getInt(1));
                System.out.println("Customer Id : " + rs.getString(2));
                System.out.println("Customer Name : " + rs.getString(3));
                System.out.println("Ordered Items : " + rs.getString(4));
                System.out.println("Total Amount : " + rs.getFloat(5));
                System.out.println("***************************");
                cnt++;
            }
            if (cnt == 0) {
                System.out.println();
                System.out.println("No Order History Available for id : " + vid);
                System.out.println();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    boolean isPaymentDone = false;

    public void paymentMethods() {
        System.out.println("Select a payment method:");
        System.out.println("1. Cash");
        System.out.println("2. Card");
        String paymentChoice = sc.next();

        switch (paymentChoice) {
            case "1":
                System.out.println("Payment completed with cash.");
                isPaymentDone = true;
                break;
            case "2":
                System.out.print("Enter card number (16 digits): ");
                String cardNumber = sc.next();
                System.out.print("Enter CVV (3 digits): ");
                String cvv = sc.next();
                System.out.print("Enter cardholder's name: ");
                String cardholderName = sc.next();

                if (isValidCard(cardNumber) && isValidCVV(cvv) && isValidCardholderName(cardholderName)) {
                    System.out.println("Payment completed with card.");
                    isPaymentDone = true;
                } else {
                    System.out.println("Invalid card information. Payment not completed.");
                }
                break;
            default:
                System.out.println("Invalid Choice");
                break;
        }
    }

    private boolean isValidCard(String cardNumber) {
        return cardNumber.length() == 16;
    }

    private boolean isValidCVV(String cvv) {
        return cvv.length() == 3;
    }

    private boolean isValidCardholderName(String cardHolderName) {
        return !cardHolderName.isEmpty();
    }

    public Queue<Order> getQueue() {
        return orderQueue;
    }

    private String getPizzaName(int itemId) {
        String itemName = null;
        String sql = "Select PizzaName from menu where PizzaId = ?";
        try {
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, itemId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                itemName = rs.getString("PizzaName");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return itemName;
    }

    private float getPizzaPrice(int itemId) {
        float amount = 0;
        String sql = "Select PizzaPrice from menu where PizzaId = ?";
        try {
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, itemId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                amount = rs.getFloat("PizzaPrice");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return amount;
    }
}

class Employee {
    Queue<Order> orderQueue;
    DBconnection db = new DBconnection();
    Connection con = db.getConnection();
    Scanner sc = new Scanner(System.in);

    Employee() {

    }

    public Employee(Queue<Order> orderQueue) {
        this.orderQueue = orderQueue;
    }

    public void addPizzaToMenu() {
        String sql = "Insert into menu(PizzaName , PizzaPrice) values(?,?)";
        try {
            PreparedStatement pst = con.prepareStatement(sql);
            System.out.print("Enter Pizza Name : ");
            String PizzaName = sc.nextLine();
            pst.setString(1, PizzaName);
            System.out.print("Enter Pizza Price : ");
            float PizzaPrice;
            try {
                PizzaPrice = Float.parseFloat(sc.nextLine());
                pst.setFloat(2, PizzaPrice);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input for Pizza Price. Please enter a valid number.");
                return;
            }
            int r = pst.executeUpdate();
            if (r > 0) {
                System.out.println(PizzaName + " Successfully Added to the Menu");
            } else {
                System.out.println("Menu Not Updated");
            }
        } catch (SQLException e) {
            System.out.println("Caught SQL Exception");
        }
    }

    public void controlQueue() {
        if (!orderQueue.isEmpty()) {
            Order order = orderQueue.peek();
            order.displayOrder();
            System.out.print("Mark this Order as Completed (Y/N): ");
            String marker = sc.next();
            if (marker.equalsIgnoreCase("Y")) {
                orderQueue.poll();
                System.out.println("Order marked as Completed and Removed From the Queue");
            } else {
                System.out.println("Order not marked as Completed");
            }
        }
        if (orderQueue.isEmpty()) {
            System.out.println("No Orders to Process");
        }
    }

    public void viewQueue() {
        if (!orderQueue.isEmpty()) {
            for (Order order : orderQueue) {
                order.displayOrder();
            }
        } else if (orderQueue.isEmpty()) {
            System.out.println("Order Queue is Empty");
        }
    }
}

class OutletMain {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        String password = "Pizza123";
        boolean b = true;
        boolean c = true;
        boolean e = true;
        Customer cust = new Customer();
        Employee emp = new Employee(cust.getQueue());
        while (b) {
            e = true;
            c = true;
            System.out.println("---Welcome To Pizza Oultet---");
            System.out.println("╔═══════════════════════════╗");
            System.out.println("║ 1) Customer               ║");
            System.out.println("║ 2) Administrator          ║");
            System.out.println("║ 3) Exit                   ║");
            System.out.println("╚═══════════════════════════╝");

            System.out.print("Enter Your Choice : ");
            String choice = sc.next();
            switch (choice) {
                case "1":
                    while (c) {
                        System.out.println("╔═══════════════════════════╗");
                        System.out.println("║ 1) Place Order            ║");
                        System.out.println("║ 2) Cancel Order           ║");
                        System.out.println("║ 3) View Order History     ║");
                        System.out.println("║ 4) Exit                   ║");
                        System.out.println("╚═══════════════════════════╝");
                        System.out.print("Enter Choice : ");
                        String cc = sc.next();
                        switch (cc) {
                            case "1":
                                cust.placeOrder();
                                break;
                            case "2":
                                cust.cancelOrder();
                                break;
                            case "3":
                                cust.viewOrderHistory();
                                break;
                            case "4":
                                c = false;
                                System.out.println("Back to Main Menu");
                                break;
                            default:
                                System.out.println("Enter Valid Option");
                                break;
                        }
                    }
                    break;
                case "2":
                    System.out.print("Enter Password to access Admin Panel : ");
                    sc.nextLine();
                    String pass = sc.nextLine();
                    if (pass.equals(password)) {
                        while (e) {
                            System.out.println("╔═══════════════════════════╗");
                            System.out.println("║ 1) Add new Pizza to Menu  ║");
                            System.out.println("║ 2) Control Queue          ║");
                            System.out.println("║ 3) View Queue             ║");
                            System.out.println("║ 4) Exit                   ║");
                            System.out.println("╚═══════════════════════════╝");
                            System.out.print("Enter Choice : ");
                            String ec = sc.next();
                            switch (ec) {
                                case "1":
                                    emp.addPizzaToMenu();
                                    break;
                                case "2":
                                    emp.controlQueue();
                                    break;
                                case "3":
                                    emp.viewQueue();
                                    break;
                                case "4":
                                    e = false;
                                    c = true;
                                    System.out.println("Back to Main Menu");
                                    break;
                                default:
                                    System.out.println("Enter Valid Option");
                                    break;
                            }
                        }
                    } else {
                        System.out.println("Enter Correct Password");
                    }
                    break;
                case "3":
                    b = false;
                    sc.close();
                    System.out.println("Exiting.......");
                    break;
                default:
                    System.out.println("Enter Valid Option(1 to 3)");
                    break;
            }
        }
    }
}

class DBconnection {
    String dburl = "jdbc:mysql://localhost:3306/Pizza Outlet";
    String dbuser = "root";
    String dbpass = "";
    String Driver = "com.mysql.cj.jdbc.Driver";

    public Connection getConnection() {
        try {
            Class.forName(Driver);
            return DriverManager.getConnection(dburl, dbuser, dbpass);
        } catch (Exception s) {
            s.printStackTrace();
            return null;
        }
    }
}